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
import org.bithon.agent.rpc.brpc.cmd.IProfilingCommand;
import org.bithon.agent.rpc.brpc.profiling.ProfilingRequest;
import org.bithon.agent.rpc.brpc.profiling.ProfilingResponse;
import org.bithon.component.brpc.StreamResponse;
import org.bithon.component.brpc.channel.BrpcServer;
import org.bithon.component.brpc.exception.SessionNotFoundException;
import org.bithon.component.brpc.message.Headers;
import org.bithon.server.agent.controller.service.AgentControllerServer;
import org.bithon.server.discovery.client.DiscoveredServiceInvoker;
import org.bithon.server.web.service.agent.api.AgentDiagnosisApi;
import org.bithon.shaded.com.google.protobuf.GeneratedMessageV3;
import org.bithon.shaded.com.google.protobuf.util.JsonFormat;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 21/5/25 6:45 pm
 */
@Slf4j
@CrossOrigin
@RestController
public class JfrApi {

    private final DiscoveredServiceInvoker discoveredServiceInvoker;
    private final ApplicationContext applicationContext;
    private final AgentControllerServer agentControllerServer;

    public JfrApi(DiscoveredServiceInvoker discoveredServiceInvoker,
                  ApplicationContext applicationContext, AgentControllerServer agentControllerServer) {
        this.discoveredServiceInvoker = discoveredServiceInvoker;
        this.applicationContext = applicationContext;
        this.agentControllerServer = agentControllerServer;
    }

    @Data
    public static class ProfileRequest {
        @NotEmpty
        private String appName;

        @NotEmpty
        private String instanceName;

        /**
         * in seconds
         */
        @Min(3)
        @Max(10)
        private long interval;

        /**
         * how long the profiling should last for in seconds
         */
        @Max(5 * 60)
        @Min(10)
        private int duration;
    }

    @GetMapping("/api/diagnosis/profiling")
    public SseEmitter profiling(@Valid @ModelAttribute AgentDiagnosisApi.ProfileRequest request) {
        SseEmitter emitter = new SseEmitter((30 + 10) * 1000L); // 30 seconds timeout

        BrpcServer.Session agentSession = agentControllerServer.getBrpcServer()
                                                               .getSessions()
                                                               .stream()
                                                               .filter((session) -> request.getAppName().equals(session.getRemoteApplicationName())
                                                                                    && request.getInstanceName().equals(session.getRemoteAttribute(Headers.HEADER_APP_ID)))
                                                               .findFirst()
                                                               .orElseThrow(() -> new SessionNotFoundException("No session found for target application [app=%s, instance=%s] ", request.getAppName(), request.getInstanceName()));

        IProfilingCommand agentJvmCommand = agentSession.getRemoteService(IProfilingCommand.class, 30);


        //
        // Find the controller where the target instance is connected to
        //
//        DiscoveredServiceInstance controller;
//        {
//            AtomicReference<DiscoveredServiceInstance> controllerRef = new AtomicReference<>();
//            List<DiscoveredServiceInstance> controllerList = discoveredServiceInvoker.getInstanceList(IAgentControllerApi.class);
//            CountDownLatch countDownLatch = new CountDownLatch(controllerList.size());
//            for (DiscoveredServiceInstance controllerInstance : controllerList) {
//                discoveredServiceInvoker.getExecutor()
//                                        .submit(() -> discoveredServiceInvoker.createUnicastApi(IAgentControllerApi.class, () -> controllerInstance)
//                                                                              .getAgentInstanceList(request.getAppName(), request.getInstanceName()))
//                                        .thenAccept((returning) -> {
//                                            if (!returning.isEmpty()) {
//                                                controllerRef.set(controllerInstance);
//                                            }
//                                        })
//                                        .whenComplete((ret, ex) -> countDownLatch.countDown());
//            }
//
//            try {
//                countDownLatch.await();
//            } catch (InterruptedException e) {
//                emitter.completeWithError(new RuntimeException(e));
//                return emitter;
//            }
//            if (controllerRef.get() == null) {
//                emitter.completeWithError(new HttpMappableException(HttpStatus.NOT_FOUND.value(), "No controller found for application instance [appName = %s, instanceName = %s]", request.getAppName(), request.getInstanceName()));
//                return emitter;
//            }
//            controller = controllerRef.get();
//        }
//
//        //
//        // Create service proxy to agent via controller
//        //
//        AgentServiceProxyFactory agentServiceProxyFactory = new AgentServiceProxyFactory(discoveredServiceInvoker, applicationContext);
//        IProfilingCommand agentJvmCommand = agentServiceProxyFactory.createUnicastProxy(IProfilingCommand.class,
//                                                                                        controller,
//                                                                                        request.getAppName(),
//                                                                                        request.getInstanceName());

        ProfilingRequest req = ProfilingRequest.newBuilder()
                                               .setDurationInSeconds(30)
                                               .setIntervalInSeconds(3)
                                               .build();
        agentJvmCommand.start(req, new StreamResponse<>() {
            @Override
            public void onNext(ProfilingResponse event) {
                try {
                    GeneratedMessageV3 data = switch (event.getEventCase()) {
                        case CPUUSAGE -> event.getCpuUsage();
                        case SYSTEMPROPERTIES -> event.getSystemProperties();
                        case CALLSTACKSAMPLE -> event.getCallStackSample();
                        case EVENT_NOT_SET -> null;
                    };

                    emitter.send(SseEmitter.event()
                                           .name(data.getClass().getSimpleName())
                                           .data(JsonFormat.printer().omittingInsignificantWhitespace().print(data), MediaType.TEXT_PLAIN)
                                           .build());
                } catch (IOException | IllegalStateException ignored) {
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
        });

        return emitter;
    }
}
