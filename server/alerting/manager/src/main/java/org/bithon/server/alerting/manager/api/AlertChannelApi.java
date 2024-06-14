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

package org.bithon.server.alerting.manager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.result.EvaluationResult;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.common.parser.AlertExpressionASTParser;
import org.bithon.server.alerting.manager.ManagerModuleEnabler;
import org.bithon.server.alerting.manager.api.parameter.ApiResponse;
import org.bithon.server.alerting.manager.biz.JsonPayloadFormatter;
import org.bithon.server.alerting.notification.channel.INotificationChannel;
import org.bithon.server.alerting.notification.channel.NotificationChannelFactory;
import org.bithon.server.alerting.notification.message.ExpressionEvaluationResult;
import org.bithon.server.alerting.notification.message.NotificationMessage;
import org.bithon.server.alerting.notification.message.OutputMessage;
import org.bithon.server.storage.alerting.IAlertNotificationChannelStorage;
import org.bithon.server.storage.alerting.IAlertObjectStorage;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.bithon.server.storage.alerting.pojo.NotificationChannelObject;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/12/22 19:12
 */
@CrossOrigin
@RestController
@Conditional(ManagerModuleEnabler.class)
public class AlertChannelApi {

    private final IAlertNotificationChannelStorage channelStorage;
    private final IAlertObjectStorage alertStorage;
    private final ObjectMapper objectMapper;

    public AlertChannelApi(IAlertNotificationChannelStorage channelStorage,
                           IAlertObjectStorage alertStorage,
                           ObjectMapper objectMapper) {
        this.channelStorage = channelStorage;
        this.alertStorage = alertStorage;
        this.objectMapper = objectMapper;
    }

    @Data
    public static class CreateChannelRequest {
        @NotEmpty
        private String name;

        @NotEmpty
        private String type;

        private Map<String, Object> props;
    }

    @PostMapping("/api/alerting/channel/create")
    public ApiResponse<?> createChannel(@Validated @RequestBody CreateChannelRequest request) throws IOException {
        if (channelStorage.exists(request.getName())) {
            throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                            "The channel with name [%s] already exists",
                                            request.getName());
        }

        String props = CollectionUtils.isEmpty(request.getProps()) ? "{}" : this.objectMapper.writeValueAsString(request.getProps());

        // Make sure the given request can be used to create a channel object correctly
        NotificationChannelFactory.create(request.getType(),
                                          request.getName(),
                                          props,
                                          this.objectMapper)
                                  .close();

        channelStorage.createChannel(request.getType(),
                                     request.getName(),
                                     props);

        return ApiResponse.success();
    }

    @PostMapping("/api/alerting/channel/test")
    public ApiResponse<Void> testChannel(@Validated @RequestBody CreateChannelRequest request) throws Exception {
        String props = CollectionUtils.isEmpty(request.getProps()) ? "{}" : this.objectMapper.writeValueAsString(request.getProps());
        try (INotificationChannel channel = NotificationChannelFactory.create(request.getType(),
                                                                              request.getName(),
                                                                              props,
                                                                              this.objectMapper)) {
            channel.send(NotificationMessage.builder()
                                            .alertRecordId("fake")
                                            .expressions(Collections.singletonList((AlertExpression) AlertExpressionASTParser.parse("count(jvm-metrics.processCpuLoad)[1m] > 1")))
                                            .conditionEvaluation(ImmutableMap.of("1", new ExpressionEvaluationResult(
                                                EvaluationResult.MATCHED,
                                                new OutputMessage("1", "2", "1")
                                            )))
                                            .alertRule(AlertRule.builder()
                                                                .id("fake")
                                                                .name("Notification Channel Test")
                                                                .expr("avg(processCpuLoad) > 1")
                                                                .build())
                                            .build());
        }

        return ApiResponse.success();
    }

    @Data
    public static class DeleteChannelRequest {
        @NotEmpty
        private String name;
    }

    @PostMapping("/api/alerting/channel/delete")
    public ApiResponse<?> deleteChannel(@Validated @RequestBody DeleteChannelRequest request) {
        // Check if it's used
        List<AlertStorageObject> alerts = alertStorage.getAlertListByTime(new Timestamp(0), new Timestamp(System.currentTimeMillis()));
        for (AlertStorageObject alert : alerts) {
            if (alert.getPayload().getNotifications().contains(request.getName())) {
                return ApiResponse.fail(StringUtils.format("The notification channel can't be deleted because it's used by alert [%s].", alert.getName()));
            }
        }

        channelStorage.deleteChannel(request.getName());
        return ApiResponse.success();
    }

    @Data
    public static class GetChannelRequest {
        /**
         * The format of the returned props
         * Can be either one of yaml/json
         */
        @NotBlank
        private String format = "json";
    }

    @Getter
    @AllArgsConstructor
    public static class GetChannelResponse {
        private int total;
        private List<Map<String, Object>> rows;
    }

    @PostMapping("/api/alerting/channel/get")
    public GetChannelResponse getChannels(@Validated @RequestBody GetChannelRequest request) {
        JsonPayloadFormatter formatter = JsonPayloadFormatter.get(request.getFormat());

        List<Map<String, Object>> channels = this.channelStorage.getChannels(0)
                                                                .stream()
                                                                .map((obj) -> {
                                                                    // Use LinkedHashMap to keep order
                                                                    Map<String, Object> map = new LinkedHashMap<>();
                                                                    map.put("name", obj.getName());
                                                                    map.put("type", obj.getType());
                                                                    map.put("props", formatter.format(obj.getPayload(), this.objectMapper, null));
                                                                    map.put("createdAt", obj.getCreatedAt());
                                                                    return map;
                                                                })
                                                                .collect(Collectors.toList());
        return new GetChannelResponse(channels.size(), channels);
    }

    @Data
    @AllArgsConstructor
    public static class GetChannelNamesResponse {
        private List<String> channels;
    }

    @PostMapping("/api/alerting/channel/names")
    public GetChannelNamesResponse getChannelNames() {
        List<String> channels = this.channelStorage.getChannels(0)
                                                   .stream()
                                                   .map(NotificationChannelObject::getName)
                                                   .collect(Collectors.toList());
        return new GetChannelNamesResponse(channels);
    }

    /**
     * An API that can be used for the notification test
     */
    @PostMapping("/api/alerting/channel/blackhole")
    public void blackHole(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String body = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
        response.getWriter().write(body);
        response.setStatus(HttpStatus.OK.value());
    }
}
