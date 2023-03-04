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

import org.bithon.agent.rpc.brpc.cmd.IConfigCommand;
import org.bithon.agent.rpc.brpc.cmd.IJvmCommand;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.brpc.exception.SessionNotFoundException;
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
    public ServiceResponse<InstanceRecord> getClients() {
        return ServiceResponse.success(commandService.getServerChannel()
                                                     .getSessions()
                                                     .stream()
                                                     .map((session) -> {
                                                         InstanceRecord instance = new InstanceRecord();
                                                         instance.setAppName(session.getAppName());
                                                         instance.setAppId(session.getAppId());
                                                         instance.setEndpoint(session.getEndpoint().toString());
                                                         return instance;
                                                     })
                                                     .collect(Collectors.toList()));
    }

    @Override
    public ServiceResponse<ThreadRecord> getStackTrace(@Valid @RequestBody CommandArgs<Void> args) {
        IJvmCommand command = commandService.getServerChannel()
                                            .getRemoteService(args.getAppId(), IJvmCommand.class, 30_000);

        return ServiceResponse.success(command.dumpThreads()
                                              .stream()
                                              .map((threadInfo) -> {
                                                  ThreadRecord thread = new ThreadRecord();
                                                  thread.setName(threadInfo.getName());
                                                  thread.setThreadId(threadInfo.getThreadId());
                                                  thread.setState(threadInfo.getState());
                                                  thread.setPriority(threadInfo.getPriority());
                                                  thread.setCpuTime(threadInfo.getCpuTime());
                                                  thread.setUserTime(threadInfo.getUserTime());
                                                  thread.setDaemon(threadInfo.isDaemon() ? 1 : 0);
                                                  thread.setWaitedCount(threadInfo.getWaitedCount());
                                                  thread.setWaitedTime(threadInfo.getWaitedTime());
                                                  thread.setBlockedCount(threadInfo.getBlockedCount());
                                                  thread.setBlockedTime(threadInfo.getBlockedTime());
                                                  thread.setLockName(threadInfo.getLockName());
                                                  thread.setLockOwnerId(threadInfo.getLockOwnerId());
                                                  thread.setLockOwnerName(threadInfo.getLockOwnerName());
                                                  thread.setInNative(threadInfo.getInNative());
                                                  thread.setSuspended(threadInfo.getSuspended());
                                                  thread.setStack(threadInfo.getStacks());
                                                  return thread;
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
    public ServiceResponse<ClassRecord> getClass(@Valid @RequestBody CommandArgs<Void> args) {
        IJvmCommand command = commandService.getServerChannel()
                                            .getRemoteService(args.getAppId(), IJvmCommand.class, 30_000);

        return ServiceResponse.success(command.getLoadedClassList()
                                              .stream()
                                              .map((clazzInfo) -> {
                                                  ClassRecord classRecord = new ClassRecord();
                                                  classRecord.name = clazzInfo.getName();
                                                  classRecord.classLoader = clazzInfo.getClassLoader();
                                                  classRecord.isAnnotation = clazzInfo.isAnnotation() ? 1 : 0;
                                                  classRecord.isInterface = clazzInfo.isInterface() ? 1 : 0;
                                                  classRecord.isEnum = clazzInfo.isEnum() ? 1 : 0;
                                                  classRecord.isSynthetic = clazzInfo.isSynthetic() ? 1 : 0;
                                                  return classRecord;
                                              })
                                              .collect(Collectors.toList()));
    }

    @Override
    public ServiceResponse<ConfigurationRecord> getConfiguration(@RequestBody CommandArgs<GetConfigurationRequest> args) {
        GetConfigurationRequest request = args.getArgs();
        String format = request == null ? "YAML" : request.getFormat();
        boolean isPretty = request == null ? true : request.isPretty();

        IConfigCommand command = commandService.getServerChannel().getRemoteService(args.getAppId(), IConfigCommand.class, 30_000);

        ConfigurationRecord record = new ConfigurationRecord();
        record.payload = command.getConfiguration(format, isPretty);

        return ServiceResponse.success(Collections.singletonList(record));
    }

    /**
     * Handle exception thrown in this REST controller.
     */
    @ExceptionHandler(ServiceInvocationException.class)
    ResponseEntity<ServiceResponse> handleException(HttpServletRequest request, ServiceInvocationException exception) {
        int statusCode = exception instanceof SessionNotFoundException ? HttpStatus.NOT_FOUND.value() : HttpStatus.INTERNAL_SERVER_ERROR.value();

        return ResponseEntity.status(statusCode)
                             .body(ServiceResponse.error(ServiceResponse.Error.builder()
                                                                              .uri(request.getRequestURI())
                                                                              .exception(exception.getClass().getName())
                                                                              .message(exception.getMessage())
                                                                              .build()));
    }
}
