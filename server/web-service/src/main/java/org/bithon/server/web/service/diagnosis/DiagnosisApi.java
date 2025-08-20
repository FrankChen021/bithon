/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.server.web.service.diagnosis;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bithon.agent.rpc.brpc.cmd.IJvmCommand;
import org.bithon.agent.rpc.brpc.cmd.IProfilingCommand;
import org.bithon.agent.rpc.brpc.profiling.ProfilingEvent;
import org.bithon.agent.rpc.brpc.profiling.ProfilingRequest;
import org.bithon.component.brpc.StreamResponse;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.forbidden.SuppressForbidden;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.discovery.client.DiscoveredServiceInstance;
import org.bithon.server.discovery.client.DiscoveredServiceInvoker;
import org.bithon.server.discovery.declaration.controller.IAgentControllerApi;
import org.bithon.server.web.service.agent.sql.table.AgentServiceProxyFactory;
import org.bithon.shaded.com.google.protobuf.GeneratedMessageV3;
import org.bithon.shaded.com.google.protobuf.util.JsonFormat;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author frank.chen021@outlook.com
 * @date 21/5/25 6:45 pm
 */
@Slf4j
@CrossOrigin
@RestController
public class DiagnosisApi {

    private final DiscoveredServiceInvoker discoveredServiceInvoker;
    private final ApplicationContext applicationContext;

    public DiagnosisApi(DiscoveredServiceInvoker discoveredServiceInvoker,
                        ApplicationContext applicationContext) {
        this.discoveredServiceInvoker = discoveredServiceInvoker;
        this.applicationContext = applicationContext;
    }

    @Data
    public static class ContinuousDumpThreadRequest {
        @NotEmpty
        private String appName;

        @NotEmpty
        private String instanceName;

        /**
         * in seconds
         */
        @Min(3)
        @Max(10)
        private int interval;

        /**
         * how long the profiling should last for in seconds
         */
        @Max(5 * 60)
        @Min(10)
        private int duration;
    }

    @SuppressForbidden
    @GetMapping("/api/diagnosis/continuous-dump-thread")
    public SseEmitter profile(@Valid @ModelAttribute ContinuousDumpThreadRequest request) {
        //
        // Find the controller where the target instance is connected to
        //
        DiscoveredServiceInstance controller;
        {
            AtomicReference<DiscoveredServiceInstance> controllerRef = new AtomicReference<>();
            List<DiscoveredServiceInstance> controllerList = discoveredServiceInvoker.getInstanceList(IAgentControllerApi.class);
            CountDownLatch countDownLatch = new CountDownLatch(controllerList.size());
            for (DiscoveredServiceInstance controllerInstance : controllerList) {
                discoveredServiceInvoker.getExecutor()
                                        .submit(() -> discoveredServiceInvoker.createUnicastApi(IAgentControllerApi.class, () -> controllerInstance)
                                                                              .getAgentInstanceList(request.getAppName(), request.getInstanceName()))
                                        .thenAccept((returning) -> {
                                            if (!returning.isEmpty()) {
                                                controllerRef.set(controllerInstance);
                                            }
                                        })
                                        .whenComplete((ret, ex) -> countDownLatch.countDown());
            }

            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if (controllerRef.get() == null) {
                throw new HttpMappableException(HttpStatus.NOT_FOUND.value(), "No controller found for application instance [appName = %s, instanceName = %s]", request.getAppName(), request.getInstanceName());
            }
            controller = controllerRef.get();
        }

        //
        // Create service proxy to agent via controller
        //
        AgentServiceProxyFactory agentServiceProxyFactory = new AgentServiceProxyFactory(discoveredServiceInvoker, applicationContext);
        IJvmCommand agentJvmCommand = agentServiceProxyFactory.createUnicastProxy(IJvmCommand.class,
                                                                                  controller,
                                                                                  request.getAppName(),
                                                                                  request.getInstanceName(),
                                                                                  30_000L);

        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(NamedThreadFactory.daemonThreadFactory("profiling-timer"));
        ExecutorService profilingExecutor = new ThreadPoolExecutor(1,
                                                                   1,
                                                                   0L,
                                                                   TimeUnit.MILLISECONDS,
                                                                   new LinkedBlockingQueue<>(1),
                                                                   NamedThreadFactory.daemonThreadFactory("profiling"),
                                                                   new ThreadPoolExecutor.DiscardPolicy());

        Runnable stopProfiling = () -> {
            timer.shutdown();
            profilingExecutor.shutdown();
        };

        SseEmitter emitter = new SseEmitter(request.getDuration() * 1000L + 500);
        emitter.onCompletion(stopProfiling);
        emitter.onTimeout(stopProfiling);
        emitter.onError((e) -> stopProfiling.run());

        //
        // Schedule a task to get information from the target instance continuously
        //
        final long duration = request.getDuration();
        timer.scheduleAtFixedRate(new Runnable() {
                                      private int elapsed = 0;

                                      @Override
                                      public void run() {
                                          try {
                                              emitter.send(SseEmitter.event()
                                                                     .id(String.valueOf(elapsed))
                                                                     .name("timer")
                                                                     .data(Map.of("elapsed", elapsed,
                                                                                  "remaining", duration - elapsed),
                                                                           MediaType.APPLICATION_JSON));
                                          } catch (IOException e) {
                                              if (!e.getMessage().contains("Broken pipe")) {
                                                  emitter.completeWithError(e);
                                              }
                                              stopProfiling.run();
                                              return;
                                          }

                                          if (elapsed % request.getInterval() == 0) {

                                              CompletableFuture.supplyAsync(agentJvmCommand::dumpThreads, profilingExecutor)
                                                               .thenAccept(threadInfos -> {
                                                                   SseEmitter.SseEventBuilder builder = SseEmitter.event()
                                                                                                                  .id(String.valueOf(elapsed))
                                                                                                                  .name("thread")
                                                                                                                  .data(threadInfos, MediaType.APPLICATION_JSON);
                                                                   try {
                                                                       emitter.send(builder);
                                                                   } catch (IllegalStateException e) {
                                                                       if (e.getMessage() == null || !e.getMessage().contains("completed")) {
                                                                           // the exception which is not thrown by SseEmitter
                                                                           throw e;
                                                                       }

                                                                       // The emitter has been completed, ignore the exception
                                                                       // This is because the above send is executed in a different thread,
                                                                       // when it's the last round to executed, the following code might close the emitter already
                                                                   } catch (IOException e) {
                                                                       // Ignore the broken pipe exception which is expected when the client closes the connection
                                                                       if (!e.getMessage().contains("Broken pipe")) {
                                                                           emitter.completeWithError(e);
                                                                       }
                                                                       stopProfiling.run();
                                                                   }
                                                               }).exceptionally((ex) -> {
                                                                   if (ex.getCause() != null) {
                                                                       ex = ex.getCause();
                                                                   }
                                                                   stopProfiling.run();
                                                                   emitter.completeWithError(ex);
                                                                   log.error("Failed to get thread info", ex);

                                                                   return null;
                                                               });
                                          }

                                          if (elapsed++ >= duration) {
                                              emitter.complete();
                                              stopProfiling.run();
                                          }
                                      }
                                  },
                                  0,
                                  1,
                                  TimeUnit.SECONDS);

        return emitter;
    }

    @Data
    public static class ContinuousProfilingRequest {
        @NotEmpty
        private String appName;

        @NotEmpty
        private String instanceName;

        /**
         * in seconds
         */
        @Min(3)
        @Max(10)
        private int interval;

        /**
         * how long the profiling should last for in seconds
         */
        @Max(5 * 60)
        @Min(10)
        private int duration;

        /**
         * Can be null or empty. if so, it defaults to cpu events.
         * <p>
         * Available events: cpu|alloc|nativemem|lock|cache-misses
         */
        private Set<String> profileEvents;
    }

    @GetMapping("/api/diagnosis/continuous-profiling")
    public SseEmitter profiling(@Valid @ModelAttribute ContinuousProfilingRequest request) {
        long profilingTaskTimeout = (request.getDuration() + request.getInterval() + 5) * 1000L; // 5 seconds more than the profiling duration
        SseEmitter emitter = new SseEmitter(profilingTaskTimeout);

        //
        // Find the controller where the target instance is connected to
        //
        DiscoveredServiceInstance controller;
        {
            AtomicReference<DiscoveredServiceInstance> controllerRef = new AtomicReference<>();
            List<DiscoveredServiceInstance> controllerList = discoveredServiceInvoker.getInstanceList(IAgentControllerApi.class);
            CountDownLatch countDownLatch = new CountDownLatch(controllerList.size());
            for (DiscoveredServiceInstance controllerInstance : controllerList) {
                discoveredServiceInvoker.getExecutor()
                                        .submit(() -> discoveredServiceInvoker.createUnicastApi(IAgentControllerApi.class, () -> controllerInstance)
                                                                              .getAgentInstanceList(request.getAppName(), request.getInstanceName()))
                                        .thenAccept((returning) -> {
                                            if (!returning.isEmpty()) {
                                                controllerRef.set(controllerInstance);
                                            }
                                        })
                                        .whenComplete((ret, ex) -> countDownLatch.countDown());
            }

            try {
                countDownLatch.await();
            } catch (InterruptedException e) {
                emitter.completeWithError(new RuntimeException(e));
                return emitter;
            }
            if (controllerRef.get() == null) {
                emitter.completeWithError(new HttpMappableException(HttpStatus.NOT_FOUND.value(), "No controller found for application instance [appName = %s, instanceName = %s]", request.getAppName(), request.getInstanceName()));
                return emitter;
            }
            controller = controllerRef.get();
        }

        //
        // Create service proxy to agent via controller
        //
        AgentServiceProxyFactory agentServiceProxyFactory = new AgentServiceProxyFactory(discoveredServiceInvoker, applicationContext);
        IProfilingCommand profilingCommand = agentServiceProxyFactory.createUnicastProxy(IProfilingCommand.class,
                                                                                         controller,
                                                                                         request.getAppName(),
                                                                                         request.getInstanceName(),
                                                                                         profilingTaskTimeout);

        ProfilingRequest profilingRequest = ProfilingRequest.newBuilder()
                                                            .setDurationInSeconds(request.getDuration())
                                                            .setIntervalInSeconds(request.getInterval())
                                                            .addAllProfileEvents(CollectionUtils.isEmpty(request.getProfileEvents()) ? List.of("cpu") : request.getProfileEvents())
                                                            .build();

        profilingCommand.start(profilingRequest, new StreamResponse<>() {
            private boolean isCancelled = false;

            @Override
            public void onNext(ProfilingEvent event) {
                try {
                    GeneratedMessageV3 data = switch (event.getEventCase()) {
                        case CPULOAD -> event.getCpuLoad();
                        case SYSTEMPROPERTIES -> event.getSystemProperties();
                        case CALLSTACKSAMPLE -> event.getCallStackSample();
                        case HEAPSUMMARY -> event.getHeapSummary();
                        case PROGRESS -> event.getProgress();
                        default -> null;
                    };

                    emitter.send(SseEmitter.event()
                                           .name(data.getClass().getSimpleName())
                                           // TODO: improve the performance of serialization 'cause the following has lower performance
                                           .data(JsonFormat.printer().omittingInsignificantWhitespace().print(data), MediaType.TEXT_PLAIN)
                                           .build());
                } catch (IOException | IllegalStateException ignored) {
                    isCancelled = true;
                }
            }

            @Override
            public void onException(Throwable throwable) {
                emitter.completeWithError(throwable);
            }

            @Override
            public void onComplete() {
                emitter.complete();
            }

            @Override
            public boolean isCancelled() {
                return isCancelled;
            }
        });

        return emitter;
    }
}
