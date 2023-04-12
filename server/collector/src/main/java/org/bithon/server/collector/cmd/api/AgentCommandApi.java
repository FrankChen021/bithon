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
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.brpc.exception.SessionNotFoundException;
import org.bithon.component.brpc.message.in.ServiceRequestMessageIn;
import org.bithon.component.brpc.message.out.ServiceRequestMessageOut;
import org.bithon.component.brpc.message.out.ServiceResponseMessageOut;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.server.collector.cmd.api.permission.PermissionConfiguration;
import org.bithon.server.collector.cmd.api.permission.PermissionRule;
import org.bithon.server.collector.cmd.service.AgentCommandService;
import org.bithon.server.discovery.declaration.ServiceResponse;
import org.bithon.server.discovery.declaration.cmd.IAgentCommandApi;
import org.bithon.shaded.com.google.protobuf.CodedInputStream;
import org.bithon.shaded.com.google.protobuf.CodedOutputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    public AgentCommandApi(ObjectMapper objectMapper,
                           AgentCommandService commandService,
                           PermissionConfiguration permissionConfiguration) {
        this.objectMapper = objectMapper;
        this.commandService = commandService;
        this.permissionConfiguration = permissionConfiguration;
    }

    @Override
    public ServiceResponse<AgentInstanceRecord> getAgentInstanceList() {
        return ServiceResponse.success(commandService.getServerChannel()
                                                     .getSessions()
                                                     .stream()
                                                     .map((session) -> {
                                                         AgentInstanceRecord instance = new AgentInstanceRecord();
                                                         instance.setAppName(session.getAppName());
                                                         instance.setAgentId(session.getAppId());
                                                         instance.setEndpoint(session.getEndpoint().toString());
                                                         instance.setAgentVersion(session.getClientVersion());
                                                         return instance;
                                                     })
                                                     .collect(Collectors.toList()));
    }

    @Override
    public byte[] proxy(@RequestParam(name = "agentId") String agentId,
                        @RequestParam(name = "token") String token,
                        @RequestBody byte[] body) throws IOException {
        //
        // Parse input request stream
        //
        CodedInputStream input = CodedInputStream.newInstance(body);
        input.pushLimit(body.length);
        ServiceRequestMessageIn fromClient = ServiceRequestMessageIn.from(input);

        // Verify if the given token matches
        // By default, if a method that starts with 'get' or 'dump' will be seen as a READ method that requires no permission check.
        if (!fromClient.getMethodName().startsWith("get") && !fromClient.getMethodName().startsWith("dump")) {
            Optional<PermissionRule> applicationRule = permissionConfiguration.getRules()
                                                                              .stream()
                                                                              .filter((rule) -> rule.getApplicationMatcher(objectMapper).matches(fromClient.getAppName()))
                                                                              .findFirst();
            if (!applicationRule.isPresent()) {
                throw new HttpMappableException(HttpStatus.FORBIDDEN.value(), "Application [%s] does not define a permission rule.", fromClient.getAppName());
            }

            if (!applicationRule.get().getToken().equals(token)) {
                throw new HttpMappableException(HttpStatus.FORBIDDEN.value(), "Given token does not match.");
            }
        }

        // Turn the input request stream to the request that is going to send to remote
        ServiceRequestMessageOut toTarget = ServiceRequestMessageOut.builder()
                                                                    .applicationName(fromClient.getAppName())
                                                                    .headers(fromClient.getHeaders())
                                                                    .isOneway(false)
                                                                    .messageType(fromClient.getMessageType())
                                                                    .serviceName(fromClient.getServiceName())
                                                                    .methodName(fromClient.getMethodName())
                                                                    .transactionId(fromClient.getTransactionId())
                                                                    .serializer(fromClient.getSerializer())
                                                                    .rawArgs(fromClient.getRawArgs())
                                                                    .build();

        // Forwarding the request to agent and wait for response
        ServiceResponseMessageOut.Builder responseBuilder = ServiceResponseMessageOut.builder()
                                                                                     .txId(fromClient.getTransactionId());
        try {
            responseBuilder.returningRaw(commandService.getServerChannel()
                                                       .getSession(agentId)
                                                       .getClientInvocation()
                                                       .invoke(toTarget, 30_000));
        } catch (Throwable e) {
            responseBuilder.exception(e.getMessage());
        } finally {
            responseBuilder.serverResponseAt(System.currentTimeMillis());
        }

        // Turn the response stream from agent into stream that is going to send back
        ServiceResponseMessageOut toClient = responseBuilder.build();
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream(512)) {
            CodedOutputStream outputStream = CodedOutputStream.newInstance(stream);
            toClient.encode(outputStream);
            return stream.toByteArray();
        }
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
