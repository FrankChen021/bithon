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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.agent.rpc.brpc.cmd.IConfigurationCommand;
import org.bithon.agent.rpc.brpc.cmd.IInstrumentationCommand;
import org.bithon.agent.rpc.brpc.cmd.IJvmCommand;
import org.bithon.agent.rpc.brpc.cmd.ILoggingCommand;
import org.bithon.component.brpc.channel.ServerChannel;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.brpc.exception.SessionNotFoundException;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.logging.LoggerConfiguration;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.collector.cmd.api.permission.PermissionConfiguration;
import org.bithon.server.collector.cmd.api.permission.PermissionRule;
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
import java.util.List;
import java.util.Optional;
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

    private final ObjectMapper objectMapper;
    private final AgentCommandService commandService;
    private final PermissionConfiguration permissionConfiguration;

    public AgentCommandApi(ObjectMapper objectMapper, AgentCommandService commandService, PermissionConfiguration permissionConfiguration) {
        this.objectMapper = objectMapper;
        this.commandService = commandService;
        this.permissionConfiguration = permissionConfiguration;
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
                                                         instance.setAgentVersion(session.getClientVersion());
                                                         return instance;
                                                     })
                                                     .collect(Collectors.toList()));
    }

    @Override
    public ServiceResponse<ThreadRecord> getThreads(@RequestBody CommandArgs<Void> args) {
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
    public ServiceResponse<ClassRecord> getClassList(@Valid @RequestBody CommandArgs<Void> args) {
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

        IConfigurationCommand command = commandService.getServerChannel().getRemoteService(args.getAppId(), IConfigurationCommand.class, 30_000);

        ConfigurationRecord record = new ConfigurationRecord();
        record.payload = command.getConfiguration(format, isPretty);

        return ServiceResponse.success(Collections.singletonList(record));
    }

    @Override
    public ServiceResponse<InstrumentedMethodRecord> getInstrumentedMethod(CommandArgs<Void> args) {
        IInstrumentationCommand command = commandService.getServerChannel()
                                                        .getRemoteService(args.getAppId(), IInstrumentationCommand.class, 30_000);

        List<InstrumentedMethodRecord> records = command.getInstrumentedMethods()
                                                        .stream()
                                                        .map((method) -> {
                                                            InstrumentedMethodRecord record = new InstrumentedMethodRecord();
                                                            record.clazzName = method.getClazzName();
                                                            record.isStatic = method.isStatic();
                                                            record.parameters = method.getParameters();
                                                            record.methodName = method.getMethodName();
                                                            record.returnType = method.getReturnType();
                                                            record.interceptor = method.getInterceptor();
                                                            return record;
                                                        }).collect(Collectors.toList());

        return ServiceResponse.success(records);
    }

    @Override
    public ServiceResponse<LoggerConfigurationRecord> getLoggerList(@RequestBody CommandArgs<Void> args) {
        List<LoggerConfiguration> loggers = commandService.getServerChannel()
                                                          .getRemoteService(args.getAppId(),
                                                                            ILoggingCommand.class,
                                                                            30_000)
                                                          .getLoggers();

        List<LoggerConfigurationRecord> result = loggers.stream()
                                                        .map((c) -> new LoggerConfigurationRecord(c.getName(),
                                                                                                  c.getLevel() == null ? null : c.getLevel().toString(),
                                                                                                  c.getEffectiveLevel() == null ? null : c.getEffectiveLevel().toString()))
                                                        .collect(Collectors.toList());
        return ServiceResponse.success(result);
    }

    @Override
    public ServiceResponse<ModifiedRecord> setLogger(@RequestBody CommandArgs<SetLoggerArgs> args) {
        Preconditions.checkArgumentNotNull("args", args.getArgs());
        Preconditions.checkNotNull("token", args.getToken());

        // Find session
        Optional<ServerChannel.Session> session = commandService.getServerChannel()
                                                                .getSessions().stream().filter((s) -> s.getAppId().equals(args.getAppId()))
                                                                .findFirst();
        if (!session.isPresent()) {
            throw new SessionNotFoundException("Can't find any connected remote application [%s] on this server.", args.getAppId());
        }

        // Check token
        final String appName = session.get().getAppName();
        Optional<PermissionRule> applicationRule = permissionConfiguration.getRules()
                                                                          .stream()
                                                                          .filter((rule) -> rule.getApplicationMatcher(objectMapper).matches(appName))
                                                                          .findFirst();
        if (!applicationRule.isPresent()) {
            throw new HttpMappableException(HttpStatus.FORBIDDEN.value(), "Application [%s] does not define a permission rule.", appName);
        }

        if (!applicationRule.get().getToken().equals(args.getToken())) {
            throw new HttpMappableException(HttpStatus.FORBIDDEN.value(), "Given token does not match.");
        }

        int rows = session.get()
                          .getRemoteService(ILoggingCommand.class, 30_000)
                          .setLogger(args.getArgs().getName(), args.getArgs().getLevel());
        ModifiedRecord modifiedRecord = new ModifiedRecord();
        modifiedRecord.setRows(rows);
        return ServiceResponse.success(Collections.singletonList(modifiedRecord));
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
