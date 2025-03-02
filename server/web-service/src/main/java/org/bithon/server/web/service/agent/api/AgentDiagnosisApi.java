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

package org.bithon.server.web.service.agent.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlCharStringLiteral;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlLiteral;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.SqlUpdate;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.util.SqlBasicVisitor;
import org.apache.calcite.sql.validate.SqlValidatorException;
import org.apache.calcite.util.NlsString;
import org.bithon.agent.rpc.brpc.cmd.IJvmCommand;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.forbidden.SuppressForbidden;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.discovery.client.DiscoveredServiceInstance;
import org.bithon.server.discovery.client.DiscoveredServiceInvoker;
import org.bithon.server.discovery.declaration.controller.IAgentControllerApi;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.bithon.server.web.service.agent.sql.AgentSchema;
import org.bithon.server.web.service.agent.sql.table.AgentServiceProxyFactory;
import org.bithon.server.web.service.agent.sql.table.IPushdownPredicateProvider;
import org.bithon.server.web.service.common.output.IOutputFormatter;
import org.bithon.server.web.service.common.output.JsonCompactOutputFormatter;
import org.bithon.server.web.service.common.output.TabSeparatedOutputFormatter;
import org.bithon.server.web.service.common.sql.SqlExecutionContext;
import org.bithon.server.web.service.common.sql.SqlExecutionEngine;
import org.bithon.server.web.service.common.sql.SqlExecutionResult;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
 * @author Frank Chen
 * @date 23/2/23 9:10 pm
 */
@Slf4j
@CrossOrigin
@RestController
@Conditional(WebServiceModuleEnabler.class)
public class AgentDiagnosisApi {

    private final SqlExecutionEngine sqlExecutionEngine;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;
    private final DiscoveredServiceInvoker discoveredServiceInvoker;

    public AgentDiagnosisApi(DiscoveredServiceInvoker discoveredServiceInvoker,
                             SqlExecutionEngine sqlExecutionEngine,
                             ObjectMapper objectMapper,
                             ApplicationContext applicationContext) {
        this.objectMapper = objectMapper;
        this.sqlExecutionEngine = sqlExecutionEngine;
        this.sqlExecutionEngine.addSchema("agent", new AgentSchema(discoveredServiceInvoker, applicationContext));
        this.applicationContext = applicationContext;
        this.discoveredServiceInvoker = discoveredServiceInvoker;
    }

    @PostMapping(value = "/api/agent/query")
    public void query(@Valid @RequestBody String query,
                      HttpServletRequest httpServletRequest,
                      HttpServletResponse httpResponse) throws Exception {
        if ("application/x-www-form-urlencoded".equals(httpServletRequest.getHeader("Content-Type"))) {
            throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                            "Content-Type with application/x-www-form-urlencoded is not accepted. Please use text/plain instead.");
        }

        SqlExecutionResult result = this.sqlExecutionEngine.executeSql(query, (sqlNode, queryContext) -> {
            SqlNode whereNode;
            SqlNode from = null;
            if (sqlNode.getKind() == SqlKind.ORDER_BY) {
                whereNode = ((SqlSelect) ((SqlOrderBy) sqlNode).query).getWhere();
                from = ((SqlSelect) ((SqlOrderBy) sqlNode).query).getFrom();
            } else if (sqlNode.getKind() == SqlKind.SELECT) {
                whereNode = ((SqlSelect) (sqlNode)).getWhere();
                from = ((SqlSelect) (sqlNode)).getFrom();
            } else if (sqlNode.getKind() == SqlKind.UPDATE) {
                whereNode = ((SqlUpdate) (sqlNode)).getCondition();
            } else {
                throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(), "Unsupported SQL Kind: %s", sqlNode.getKind());
            }

            Map<String, Boolean> pushdownPredicates = Collections.emptyMap();
            if (from != null) {
                if (!(from instanceof SqlIdentifier)) {
                    throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(), "Not supported '%s'. The 'from' clause can only be an identifier", from.toString());
                }

                List<String> names = ((SqlIdentifier) from).names;
                if (names.size() != 2) {
                    throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(), "Unknown identifier: %s", from.toString());
                }

                Schema schema = queryContext.getRootSchema().getSubSchema(names.get(0));
                if (schema == null) {
                    throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(), "Unknown schema: %s", names.get(0));
                }

                Table table = schema.getTable(names.get(1));
                if (table == null) {
                    throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(), "Unknown table: %s", names.get(1));
                }
                if (table instanceof IPushdownPredicateProvider) {
                    pushdownPredicates = ((IPushdownPredicateProvider) table).getPredicates();
                }
            }
            if (whereNode != null) {
                // Convert related filter at the raw SQL into query context parameters
                whereNode.accept(new FilterToContextParameterConverter(queryContext, pushdownPredicates));
            }

            if (!pushdownPredicates.isEmpty()) {
                pushdownPredicates.forEach((predicate, required) -> {
                    if (required && queryContext.get(predicate) == null) {
                        throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                                        "Missing filter on '%s' in the given SQL. Please update your SQL to continue.",
                                                        predicate);
                    }
                });
            }
        });

        IOutputFormatter formatter;
        String acceptEncoding = httpServletRequest.getHeader("Accept");
        if (acceptEncoding != null && acceptEncoding.contains("application/json")) {
            formatter = new JsonCompactOutputFormatter(this.objectMapper);
        } else {
            formatter = new TabSeparatedOutputFormatter();
        }
        httpResponse.addHeader("Content-Type", formatter.getContentType());
        formatter.format(httpResponse.getWriter(), result.fields, result.rows);
    }

    private static class FilterToContextParameterConverter extends SqlBasicVisitor<String> {
        private final SqlExecutionContext queryContext;
        private final Map<String, Boolean> pushDownPredicates;

        public FilterToContextParameterConverter(SqlExecutionContext queryContext, Map<String, Boolean> pushDownPredicates) {
            this.queryContext = queryContext;
            this.pushDownPredicates = pushDownPredicates;
        }

        @Override
        public String visit(SqlCall call) {
            if (!(call instanceof SqlBasicCall)) {
                return super.visit(call);
            }

            if (!"=".equals(call.getOperator().getName())) {
                return super.visit(call);
            }

            if (call.getOperandList().size() != 2) {
                return super.visit(call);
            }
            SqlNode identifierNode = call.getOperandList().get(0);
            SqlNode literal = call.getOperandList().get(1);
            if (!(identifierNode instanceof SqlIdentifier)) {
                SqlNode tmp = literal;
                literal = identifierNode;
                identifierNode = tmp;
            }
            if (!(identifierNode instanceof SqlIdentifier)) {
                return super.visit(call);
            }

            String identifier = ((SqlIdentifier) identifierNode).getSimple();
            if (!pushDownPredicates.containsKey(identifier)) {
                return super.visit(call);
            }

            if (!(literal instanceof SqlCharStringLiteral)) {
                throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                                StringUtils.format("Operand for [%s] must be type of STRING", identifier));
            }

            // Set the instance/_token in the execution context
            this.queryContext.set(identifier, ((SqlCharStringLiteral) literal).getValueAs(NlsString.class).getValue());

            // Replace current filter expression by '1 = 1'
            call.setOperand(0, SqlLiteral.createBoolean(true, new SqlParserPos(-1, -1)));
            call.setOperand(1, SqlLiteral.createBoolean(true, new SqlParserPos(-1, -1)));
            return null;
        }
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

    @SuppressForbidden
    @GetMapping("/api/agent/profile/jvm")
    public SseEmitter profile(@Valid @ModelAttribute ProfileRequest request) {
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
                                                                                  request.getInstanceName());


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

                                              CompletableFuture.supplyAsync(() -> {
                                                                   List<IJvmCommand.ThreadInfo> threadInfos = agentJvmCommand.dumpThreads();
                                                                   return threadInfos;
                                                               }, profilingExecutor)
                                                               .thenAccept(threadInfos -> {
                                                                   try {
                                                                       emitter.send(SseEmitter.event()
                                                                                              .id(String.valueOf(elapsed))
                                                                                              .name("thread")
                                                                                              .data(threadInfos, MediaType.APPLICATION_JSON));
                                                                   } catch (IOException e) {
                                                                       if (!e.getMessage().contains("Broken pipe")) {
                                                                           emitter.completeWithError(e);
                                                                       }
                                                                       stopProfiling.run();
                                                                   }
                                                               }).exceptionally((ex) -> {
                                                                   if (ex != null) {
                                                                       stopProfiling.run();

                                                                       log.error("Failed to get thread info", ex);
                                                                   }

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

    @ExceptionHandler({SqlValidatorException.class, SqlParseException.class})
    void suppressSqlException(HttpServletResponse response, Exception e) throws IOException {
        response.setContentType("text/plain");
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.getWriter().write(e.getMessage());
    }
}
