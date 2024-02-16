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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.brpc.channel.BrpcServer;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.brpc.exception.SessionNotFoundException;
import org.bithon.component.brpc.message.Headers;
import org.bithon.component.brpc.message.in.ServiceRequestMessageIn;
import org.bithon.component.brpc.message.out.ServiceRequestMessageOut;
import org.bithon.component.brpc.message.out.ServiceResponseMessageOut;
import org.bithon.server.agent.controller.config.AgentControllerConfig;
import org.bithon.server.agent.controller.config.PermissionConfig;
import org.bithon.server.agent.controller.service.AgentControllerServer;
import org.bithon.server.commons.exception.ErrorResponse;
import org.bithon.server.discovery.declaration.ServiceResponse;
import org.bithon.server.discovery.declaration.controller.IAgentProxyApi;
import org.bithon.shaded.com.google.protobuf.CodedInputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.Timestamp;
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
public class AgentProxyApi implements IAgentProxyApi {

    private final ObjectMapper objectMapper;
    private final AgentControllerServer agentControllerServer;
    private final PermissionConfig permissionConfig;

    public AgentProxyApi(ObjectMapper objectMapper,
                         AgentControllerServer agentControllerServer,
                         AgentControllerConfig agentConfig) {
        this.objectMapper = objectMapper;
        this.agentControllerServer = agentControllerServer;
        this.permissionConfig = agentConfig.getPermission();
    }

    @Override
    public ServiceResponse<AgentInstanceRecord> getAgentInstanceList() {
        return ServiceResponse.success(agentControllerServer.getBrpcServer()
                                                            .getSessions()
                                                            .stream()
                                                            .map((session) -> {
                                                                AgentInstanceRecord instance = new AgentInstanceRecord();
                                                                instance.setAppName(session.getRemoteApplicationName());
                                                                instance.setInstance(session.getRemoteAttribute(Headers.HEADER_APP_ID, session.getRemoteEndpoint()));
                                                                instance.setEndpoint(session.getRemoteEndpoint());
                                                                instance.setController(session.getLocalEndpoint());
                                                                instance.setAgentVersion(session.getRemoteAttribute(Headers.HEADER_VERSION));
                                                                long start = 0;
                                                                try {
                                                                    start = Long.parseLong(session.getRemoteAttribute(Headers.HEADER_START_TIME, "0"));
                                                                } catch (NumberFormatException ignored) {
                                                                }
                                                                instance.setStartAt(new Timestamp(start).toLocalDateTime());
                                                                return instance;
                                                            })
                                                            .collect(Collectors.toList()));
    }

    @Override
    public byte[] proxy(String token, String instance, Integer timeout, byte[] rawMessage) throws IOException {
        // Get the session first
        BrpcServer.Session agentSession = agentControllerServer.getBrpcServer().getSession(instance);

        //
        // Parse input request stream
        //
        CodedInputStream input = CodedInputStream.newInstance(rawMessage);
        input.pushLimit(rawMessage.length);
        ServiceRequestMessageIn rawRequest = ServiceRequestMessageIn.from(input);

        // Verify if the given token matches
        // By default if a method that starts with 'get' or 'dump' will be seen as a READ method that requires no permission check.
        if (!rawRequest.getMethodName().startsWith("get") && !rawRequest.getMethodName().startsWith("dump")) {
            permissionConfig.verifyPermission(objectMapper, agentSession.getRemoteApplicationName(), token);
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
