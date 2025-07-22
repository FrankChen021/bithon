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

package org.bithon.server.agent.controller.api;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.http.HttpServletRequest;
import org.bithon.component.brpc.StreamResponse;
import org.bithon.component.brpc.channel.BrpcServer;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.brpc.exception.SessionNotFoundException;
import org.bithon.component.brpc.message.Headers;
import org.bithon.component.brpc.message.in.ServiceRequestMessageIn;
import org.bithon.component.brpc.message.out.ServiceRequestMessageOut;
import org.bithon.component.brpc.message.out.ServiceResponseMessageOut;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.agent.controller.config.AgentControllerConfig;
import org.bithon.server.agent.controller.config.PermissionConfig;
import org.bithon.server.agent.controller.rbac.Operation;
import org.bithon.server.agent.controller.service.AgentControllerServer;
import org.bithon.server.agent.controller.service.AgentSettingLoader;
import org.bithon.server.commons.exception.ErrorResponse;
import org.bithon.server.discovery.declaration.controller.IAgentControllerApi;
import org.bithon.server.web.security.jwt.JwtConfig;
import org.bithon.server.web.security.jwt.JwtTokenComponent;
import org.bithon.shaded.com.google.protobuf.CodedInputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The management API for agents.
 * Since the management API between agents and bithon is bored on brpc interface,
 * the management REST API is only available when the brpc is enabled.
 *
 * @author Frank Chen
 * @date 2022/8/7 20:46
 */
@RestController
@ConditionalOnProperty(value = "bithon.agent-controller.enabled", havingValue = "true")
public class AgentControllerApi implements IAgentControllerApi {

    private final AgentControllerServer agentControllerServer;
    private final PermissionConfig permissionConfig;
    private final AgentSettingLoader loader;
    private final JwtTokenComponent jwtTokenComponent;

    public AgentControllerApi(JwtConfig jwtConfig,
                              AgentControllerServer agentControllerServer,
                              AgentControllerConfig agentConfig,
                              AgentSettingLoader loader) {
        this.agentControllerServer = agentControllerServer;
        this.permissionConfig = agentConfig.getPermission();
        this.jwtTokenComponent = new JwtTokenComponent(jwtConfig);
        this.loader = loader;
    }

    @Override
    public List<AgentInstanceRecord> getAgentInstanceList(String application, String instance) {
        return agentControllerServer.getBrpcServer()
                                    .getSessions()
                                    .stream()
                                    .map((session) -> {
                                        AgentInstanceRecord record = new AgentInstanceRecord();
                                        record.setAppName(session.getRemoteApplicationName());
                                        record.setInstance(session.getRemoteAttribute(Headers.HEADER_APP_ID, session.getRemoteEndpoint()));
                                        record.setEndpoint(session.getRemoteEndpoint());
                                        record.setController(session.getLocalEndpoint());

                                        String agentVersion = session.getRemoteAttribute(Headers.HEADER_VERSION);
                                        if (agentVersion != null) {
                                            String[] parts = agentVersion.split("@");
                                            record.setAgentVersion(parts[0]);
                                            record.setBuildId(parts[1]);
                                            record.setBuildTime(parts[2]);
                                        }
                                        long start = 0;
                                        try {
                                            start = Long.parseLong(session.getRemoteAttribute(Headers.HEADER_START_TIME, "0"));
                                        } catch (NumberFormatException ignored) {
                                        }
                                        record.setStartAt(new Timestamp(start).toLocalDateTime());
                                        record.setSessionStartAt(new Timestamp(session.getSessionStartTimestamp()).toLocalDateTime());
                                        return record;
                                    })
                                    .filter((record) -> application == null || application.equals(record.getAppName())
                                                                               && (instance == null || instance.equals(record.getInstance())))
                                    .collect(Collectors.toList());
    }

    @Override
    public byte[] callAgentService(String token,
                                   String application,
                                   String instance,
                                   Integer timeout,
                                   byte[] message) throws IOException {
        // Get the session first
        BrpcServer.Session agentSession = agentControllerServer.getBrpcServer()
                                                               .getSessions()
                                                               .stream()
                                                               .filter((session) -> application.equals(session.getRemoteApplicationName())
                                                                                    && instance.equals(session.getRemoteAttribute(Headers.HEADER_APP_ID)))
                                                               .findFirst()
                                                               .orElseThrow(() -> new SessionNotFoundException("No session found for target application [app=%s, instance=%s] ", application, instance));

        //
        // Parse input request stream so that we get the request object that the user is going to access
        //
        CodedInputStream input = CodedInputStream.newInstance(message);
        input.pushLimit(message.length);
        ServiceRequestMessageIn rawRequest = ServiceRequestMessageIn.from(input);

        // Verify if the user has permission if the permission checking is ENABLE on this service
        if (permissionConfig != null && permissionConfig.isEnabled()) {

            String user;
            if (StringUtils.isEmpty(token)) {
                user = "anonymousUser";
            } else {
                Jws<Claims> parsedToken = jwtTokenComponent.tryParseToken(token);
                if (parsedToken == null) {
                    // Use HTTP 403 instead of 401
                    // Because the feign client is not able to read response body when 401 is returned.
                    // Don't know why
                    throw new HttpMappableException(HttpStatus.FORBIDDEN.value(),
                                                    "Invalid token provided to perform the operation on the agent of target application.");

                }
                user = parsedToken.getBody().getSubject();
            }

            Operation operation = rawRequest.getMethodName().startsWith("get") || rawRequest.getMethodName().startsWith("dump") ? Operation.READ : Operation.WRITE;
            permissionConfig.verifyPermission(operation,
                                              user,
                                              agentSession.getRemoteApplicationName(),
                                              // Previously,
                                              // some data sources is defined from one remote service like IJvmCommand
                                              // if we want to limit the access to one data source, we have to use the service name combined with the method name
                                              // From this point of view, at the agent side, each data source should be defined as a separate service
                                              // But now, changing/adding service name at the agent side is not easy which requires all target applications to be updated
                                              rawRequest.getServiceName() + "#" + rawRequest.getMethodName());
        }

        // Turn the input request stream to the request that is going to send to remote
        ServiceRequestMessageOut toTarget = ServiceRequestMessageOut.builder()
                                                                    .applicationName(rawRequest.getApplicationName())
                                                                    .headers(rawRequest.getHeaders())
                                                                    .isOneway(false)
                                                                    .messageType(rawRequest.getMessageType())
                                                                    .serviceName(rawRequest.getServiceName())
                                                                    .methodName(rawRequest.getMethodName())
                                                                    .transactionId(rawRequest.getTransactionId())
                                                                    .serializer(rawRequest.getSerializer())
                                                                    .rawArgs(rawRequest.getRawArgs())
                                                                    .build();

        // Forwarding the request to agent and wait for response
        ServiceResponseMessageOut.Builder responseBuilder = ServiceResponseMessageOut.builder()
                                                                                     .txId(rawRequest.getTransactionId());
        try {
            responseBuilder.returningRaw(agentSession.getLowLevelInvoker().invoke(toTarget, timeout == null ? 30_000 : timeout));
        } catch (Throwable e) {
            responseBuilder.exception(e);
        } finally {
            responseBuilder.serverResponseAt(System.currentTimeMillis());
        }

        // Turn the response stream from agent into stream that is going to send back
        return responseBuilder.build().toByteArray();
    }

    @PostMapping("/api/agent/service/proxy/streaming")
    public SseEmitter callStreamingService(@RequestHeader(name = "X-Bithon-Token", required = false) String token,
                                           @RequestParam(name = PARAMETER_NAME_APP_NAME) String application,
                                           @RequestParam(name = PARAMETER_NAME_INSTANCE) String instance,
                                           @RequestParam(name = "timeout", required = false) Integer timeout,
                                           @RequestBody byte[] message) throws IOException {
        // Get the session first
        BrpcServer.Session agentSession = agentControllerServer.getBrpcServer()
                                                               .getSessions()
                                                               .stream()
                                                               .filter((session) -> application.equals(session.getRemoteApplicationName())
                                                                                    && instance.equals(session.getRemoteAttribute(Headers.HEADER_APP_ID)))
                                                               .findFirst()
                                                               .orElseThrow(() -> new SessionNotFoundException("No session found for target application [app=%s, instance=%s] ", application, instance));

        //
        // Parse input request stream so that we get the request object that the user is going to access
        //
        CodedInputStream input = CodedInputStream.newInstance(message);
        input.pushLimit(message.length);
        ServiceRequestMessageIn rawRequest = ServiceRequestMessageIn.from(input);

        // Verify if the user has permission if the permission checking is ENABLE on this service
        if (permissionConfig != null && permissionConfig.isEnabled()) {
            String user;
            if (StringUtils.isEmpty(token)) {
                user = "anonymousUser";
            } else {
                Jws<Claims> parsedToken = jwtTokenComponent.tryParseToken(token);
                if (parsedToken == null) {
                    // Use HTTP 403 instead of 401
                    // Because the feign client is not able to read response body when 401 is returned.
                    // Don't know why
                    throw new HttpMappableException(HttpStatus.FORBIDDEN.value(),
                                                    "Invalid token provided to perform the operation on the agent of target application.");

                }
                user = parsedToken.getBody().getSubject();
            }

            Operation operation = rawRequest.getMethodName().startsWith("get") || rawRequest.getMethodName().startsWith("dump") ? Operation.READ : Operation.WRITE;
            permissionConfig.verifyPermission(operation,
                                              user,
                                              agentSession.getRemoteApplicationName(),
                                              // Previously,
                                              // some data sources is defined from one remote service like IJvmCommand
                                              // if we want to limit the access to one data source, we have to use the service name combined with the method name
                                              // From this point of view, at the agent side, each data source should be defined as a separate service
                                              // But now, changing/adding service name at the agent side is not easy which requires all target applications to be updated
                                              rawRequest.getServiceName() + "#" + rawRequest.getMethodName());
        }

        // Turn the input request stream to the request that is going to send to remote
        ServiceRequestMessageOut toTarget = ServiceRequestMessageOut.builder()
                                                                    .applicationName(rawRequest.getApplicationName())
                                                                    .headers(rawRequest.getHeaders())
                                                                    .isOneway(false)
                                                                    .messageType(rawRequest.getMessageType())
                                                                    .serviceName(rawRequest.getServiceName())
                                                                    .methodName(rawRequest.getMethodName())
                                                                    .transactionId(rawRequest.getTransactionId())
                                                                    .serializer(rawRequest.getSerializer())
                                                                    .rawArgs(rawRequest.getRawArgs())
                                                                    .build();


        SseEmitter emitter = new SseEmitter(timeout == null ? 30_000L : timeout.longValue());
        StreamResponse<byte[]> remoteResponse = new StreamResponse<>() {
            @Override
            public void onNext(byte[] data) {
                try {
                    emitter.send(SseEmitter.event().name("data").data(Base64.getEncoder().encode(data)));
                } catch (IOException ignored) {
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
        };
        try {
            agentSession.getLowLevelInvoker()
                        .invokeStreaming(toTarget,
                                         remoteResponse,
                                         timeout == null ? 30_000 : timeout);
            return emitter;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateAgentSetting(String appName, String env) {
        // TODO: call the agent RPC to update setting at agent side immediately
        this.loader.update(appName, env);
    }

    /**
     * Handle exception thrown in this REST controller.
     */
    @ExceptionHandler(ServiceInvocationException.class)
    ResponseEntity<ErrorResponse> handleException(HttpServletRequest request, ServiceInvocationException exception) {
        int statusCode = exception instanceof SessionNotFoundException ? HttpStatus.NOT_FOUND.value() : HttpStatus.INTERNAL_SERVER_ERROR.value();

        return ResponseEntity.status(statusCode)
                             .body(ErrorResponse.builder()
                                                .path(request.getRequestURI())
                                                .exception(exception.getClass().getName())
                                                .message(exception.getMessage())
                                                .build());
    }
}
