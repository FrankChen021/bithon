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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutput;
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutputs;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.common.parser.AlertExpressionASTParser;
import org.bithon.server.alerting.manager.ManagerModuleEnabler;
import org.bithon.server.alerting.manager.api.model.ApiResponse;
import org.bithon.server.alerting.notification.channel.INotificationChannel;
import org.bithon.server.alerting.notification.channel.NotificationChannelFactory;
import org.bithon.server.alerting.notification.message.NotificationMessage;
import org.bithon.server.commons.json.JsonPayloadFormatter;
import org.bithon.server.storage.alerting.IAlertNotificationChannelStorage;
import org.bithon.server.storage.alerting.IAlertObjectStorage;
import org.bithon.server.storage.alerting.Label;
import org.bithon.server.storage.alerting.pojo.AlertStatus;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.bithon.server.storage.alerting.pojo.AlertStorageObjectPayload;
import org.bithon.server.storage.alerting.pojo.NotificationChannelObject;
import org.bithon.server.storage.alerting.pojo.NotificationProps;
import org.bithon.server.storage.datasource.query.Limit;
import org.bithon.server.storage.datasource.query.OrderBy;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/12/22 19:12
 */
@Slf4j
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

    @Getter
    @Setter
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

    @Getter
    @Setter
    public static class TestChannelRequest extends CreateChannelRequest {
        // The timeout in seconds
        @Min(1)
        @Max(60)
        private int timeout = 10;
    }

    @PostMapping("/api/alerting/channel/test")
    public ApiResponse<Void> testChannel(@Validated @RequestBody TestChannelRequest request) throws Exception {
        String props = CollectionUtils.isEmpty(request.getProps()) ? "{}" : this.objectMapper.writeValueAsString(request.getProps());
        try (INotificationChannel channel = NotificationChannelFactory.create(request.getType(),
                                                                              request.getName(),
                                                                              props,
                                                                              this.objectMapper)) {
            channel.test(NotificationMessage.builder()
                                            .alertRecordId("fake")
                                            .expressions(AlertRule.flattenExpressions(AlertExpressionASTParser.parse("count(jvm-metrics.processCpuLoad)[1m] > 1")))
                                            .evaluationOutputs(ImmutableMap.of("1",
                                                                               EvaluationOutputs.of(EvaluationOutput.builder()
                                                                                                                    .matched(true)
                                                                                                                    .label(Label.EMPTY)
                                                                                                                    .current("1")
                                                                                                                    .threshold("2")
                                                                                                                    .delta("1")
                                                                                                                    .build())))
                                            .status(AlertStatus.ALERTING)
                                            .alertRule(AlertRule.builder()
                                                                .id("fake")
                                                                .name("Notification Channel Test")
                                                                .expr("avg(processCpuLoad) > 1")
                                                                .every(HumanReadableDuration.DURATION_1_MINUTE)
                                                                .notificationProps(NotificationProps.builder()
                                                                                                    .channels(Collections.singletonList(request.getName()))
                                                                                                    .silence(HumanReadableDuration.of(10, TimeUnit.MINUTES))
                                                                                                    .build())
                                                                .build())
                                            .build(),
                         Duration.ofSeconds(request.getTimeout()));
        } catch (RuntimeException e) {
            log.error(StringUtils.format("Failed to send notification to channel [%s]", request.getName()), e);
            throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                            "Failed to send notification to channel [%s]: %s",
                                            request.getName(),
                                            e.getMessage());
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
        List<AlertStorageObject> alerts = alertStorage.getRuleListByTime(new Timestamp(0), new Timestamp(System.currentTimeMillis()));
        for (AlertStorageObject alert : alerts) {
            AlertStorageObjectPayload payload = alert.getPayload();
            if (payload.getNotifications() != null && alert.getPayload().getNotifications().contains(request.getName())) {
                return ApiResponse.fail(StringUtils.format("The notification channel can't be deleted because it's used by alert [%s].", alert.getName()));
            }
            if (payload.getNotificationProps() != null && payload.getNotificationProps().getChannels().contains(request.getName())) {
                return ApiResponse.fail(StringUtils.format("The notification channel can't be deleted because it's used by alert [%s].", alert.getName()));
            }
        }

        channelStorage.deleteChannel(request.getName());
        return ApiResponse.success();
    }

    @Getter
    @Setter
    public static class UpdateChannelRequest {
        @NotBlank
        private String name;

        private Map<String, Object> props;
    }

    @PostMapping("/api/alerting/channel/update")
    public void updateChannel(@Validated @RequestBody UpdateChannelRequest request) throws IOException {
        NotificationChannelObject channel = channelStorage.getChannel(request.getName());
        if (channel == null) {
            throw new HttpMappableException(HttpStatus.NOT_FOUND.value(),
                                            "The channel with name [%s] does not exist",
                                            request.getName());
        }

        String newProps = CollectionUtils.isEmpty(request.getProps()) ? "{}" : this.objectMapper.writeValueAsString(request.getProps());

        // Make sure the given request can be used to create a channel object correctly
        NotificationChannelFactory.create(channel.getType(),
                                          request.getName(),
                                          newProps,
                                          this.objectMapper)
                                  .close();

        if (!channelStorage.updateChannel(channel, newProps)) {
            throw new HttpMappableException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                            "Failed to update the channel with name [%s]",
                                            request.getName());
        }
    }

    @Data
    public static class GetChannelListRequest {
        private String name;
        private OrderBy orderBy;
        private Limit limit;

        /**
         * The format of the returned props
         * Can be either one of yaml/json
         */
        @NotBlank
        private String format = "json";
    }

    @Getter
    @AllArgsConstructor
    public static class GetChannelListResponse {
        private int total;
        private List<Map<String, Object>> rows;
    }

    @PostMapping("/api/alerting/channel/list")
    public GetChannelListResponse getChannelList(@Validated @RequestBody GetChannelListRequest request) {
        JsonPayloadFormatter formatter = JsonPayloadFormatter.get(request.getFormat());

        IAlertNotificationChannelStorage.GetChannelRequest req = IAlertNotificationChannelStorage.GetChannelRequest.builder()
                                                                                                                   .name(request.getName())
                                                                                                                   .since(0)
                                                                                                                   .orderBy(request.getOrderBy())
                                                                                                                   .limit(request.getLimit())
                                                                                                                   .build();

        int total = this.channelStorage.getChannelsSize(req);
        List<Map<String, Object>> channels = this.channelStorage.getChannels(req)
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
        return new GetChannelListResponse(total, channels);
    }

    @Getter
    @Setter
    public static class GetChannelRequest {
        @NotBlank
        private String name;

        /**
         * The format of the returned props
         * Can be either one of yaml/json
         */
        @NotBlank
        private String format = "json";
    }

    public static class GetChannelResponse extends TreeMap<String, Object> {
    }

    @PostMapping("/api/alerting/channel/get")
    public GetChannelResponse getChannel(@Validated @RequestBody GetChannelRequest request) {
        JsonPayloadFormatter formatter = JsonPayloadFormatter.get(request.getFormat());

        NotificationChannelObject channel = this.channelStorage.getChannel(request.getName());
        if (channel == null) {
            throw new HttpMappableException(HttpStatus.NOT_FOUND.value(),
                                            "The channel with name [%s] does not exist",
                                            request.getName());
        }

        GetChannelResponse resp = new GetChannelResponse();

        resp.put("name", channel.getName());
        resp.put("type", channel.getType());
        resp.put("props", formatter.format(channel.getPayload(), this.objectMapper, null));
        resp.put("createdAt", channel.getCreatedAt());
        return resp;
    }

    @Data
    @AllArgsConstructor
    public static class GetChannelNamesResponse {
        private List<String> channels;
    }

    @PostMapping("/api/alerting/channel/names")
    public GetChannelNamesResponse getChannelNames() {
        List<String> channels = this.channelStorage.getChannels(IAlertNotificationChannelStorage.GetChannelRequest.builder()
                                                                                                                  .since(0)
                                                                                                                  .build())
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
        log.info("Received notification: {}", body);
        response.getWriter().write(body);
        response.setStatus(HttpStatus.OK.value());
    }
}
