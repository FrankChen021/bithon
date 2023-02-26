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

package org.bithon.server.collector.cmd.api;

import com.google.common.collect.ImmutableMap;
import org.bithon.agent.rpc.brpc.cmd.IConfigCommand;
import org.bithon.agent.rpc.brpc.cmd.IJvmCommand;
import org.bithon.component.brpc.channel.ServerChannel;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.brpc.exception.SessionNotFoundException;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.collector.cmd.service.AgentCommandService;
import org.bithon.server.discovery.declaration.ServiceResponse;
import org.bithon.server.discovery.declaration.cmd.CommandArgs;
import org.bithon.server.discovery.declaration.cmd.IAgentCommandApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * The management API for agents.
 * Since the management API between agents and bithon is bored on brpc interface,
 * the management REST API is only available when the brpc is enabled.
 * <p>
 * TODO: We can separate the deployment of agent management from current collector module into a independent module which can be deployed as a single application.
 *
 * @author Frank Chen
 * @date 2022/8/7 20:46
 */
@RestController
@ConditionalOnProperty(value = "collector-brpc.enabled", havingValue = "true")
public class AgentCommandApi implements IAgentCommandApi {

    private final AgentCommandService commandService;

    public AgentCommandApi(AgentCommandService commandService) {
        this.commandService = commandService;
    }

    @Override
    public ServiceResponse<Client> getClients() {
        return ServiceResponse.success(commandService.getServerChannel()
                                                     .getSessions()
                                                     .stream()
                                                     .sorted(Comparator.comparing(ServerChannel.Session::getAppName))
                                                     .map((session) -> {
                                                         Client client = new Client();
                                                         client.setAppName(session.getAppName());
                                                         client.setAppId(session.getAppId());
                                                         client.setEndpoint(session.getEndpoint().toString());
                                                         return client;
                                                     })
                                                     .collect(Collectors.toList()));
    }

    @Override
    public ServiceResponse<StackTrace> getStackTrace(@Valid @RequestBody CommandArgs<Void> args) {
        IJvmCommand command = commandService.getServerChannel().getRemoteService(args.getAppId(), IJvmCommand.class);

        return ServiceResponse.success(command.dumpThreads()
                                              .stream()
                                              // Sort by thread name for better analysis
                                              .sorted(Comparator.comparing(IJvmCommand.ThreadInfo::getName))
                                              .map((thread) -> {
                                                  StackTrace stackTrace = new StackTrace();
                                                  stackTrace.setName(thread.getName());
                                                  stackTrace.setThreadId(thread.getThreadId());
                                                  stackTrace.setState(thread.getState());
                                                  stackTrace.setPriority(thread.getPriority());
                                                  stackTrace.setCpuTime(thread.getCpuTime());
                                                  stackTrace.setUserTime(thread.getUserTime());
                                                  stackTrace.setDaemon(thread.isDaemon());
                                                  stackTrace.setStack(thread.getStacks());
                                                  return stackTrace;
                                              })
                                              .collect(Collectors.toList()));
    }

    /**
     * @param args A string pattern which comply with database's like expression.
     *             For example:
     *             "%CommandApi" will match all classes whose name ends with CommandApi
     *             "CommandApi" matches only qualified class name that is the exact CommandApi
     *             "%bithon% matches all qualified classes whose name contains bithon
     */
    @Override
    public ServiceResponse<String> getClassList(@Valid @RequestBody CommandArgs<String> args) {
        String pattern;
        if (StringUtils.isEmpty(args.getArgs())) {
            pattern = ".*";
        } else {
            pattern = args.getArgs();
            pattern = pattern.replace(".", "\\.").replace("%", ".*");
        }

        IJvmCommand command = commandService.getServerChannel().getRemoteService(args.getAppId(), IJvmCommand.class);

        return ServiceResponse.success(command.dumpClazz(pattern)
                                              .stream()
                                              .sorted()
                                              .collect(Collectors.toList()));
    }

    @Override
    public ServiceResponse<String> getConfiguration(@RequestBody CommandArgs<GetConfigurationRequest> args) {
        GetConfigurationRequest request = args.getArgs();
        String format = request == null ? "YAML" : request.getFormat();
        boolean isPretty = request == null ? true : request.isPretty();

        IConfigCommand command = commandService.getServerChannel().getRemoteService(args.getAppId(), IConfigCommand.class);

        return ServiceResponse.success(Collections.singletonList(command.getConfiguration(format, isPretty)));
    }

    /**
     * Handle exception thrown in this REST controller
     */
    @ExceptionHandler(ServiceInvocationException.class)
    public ResponseEntity<ServiceResponse> handleException(HttpServletRequest request, ServiceInvocationException exception) {
        int statusCode = exception instanceof SessionNotFoundException ? HttpStatus.NOT_FOUND.value() : HttpStatus.INTERNAL_SERVER_ERROR.value();

        return ResponseEntity.status(statusCode)
                             .body(ServiceResponse.error(ImmutableMap.of("path", request.getRequestURI(),
                                                                         "exception", exception.getClass().getName(),
                                                                         "message", exception.getMessage())));
    }
}
