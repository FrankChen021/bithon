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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.manager.ManagerModuleEnabler;
import org.bithon.server.alerting.manager.api.parameter.ApiResponse;
import org.bithon.server.alerting.notification.channel.INotificationChannel;
import org.bithon.server.storage.alerting.IAlertNotificationChannelStorage;
import org.bithon.server.storage.alerting.IAlertObjectStorage;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/12/22 19:12
 */
@CrossOrigin
@RestController
@Conditional(ManagerModuleEnabler.class)
public class AlertNotificationApi {

    private final IAlertNotificationChannelStorage channelStorage;
    private final IAlertObjectStorage alertStorage;
    private final ObjectMapper objectMapper;

    public AlertNotificationApi(IAlertNotificationChannelStorage channelStorage,
                                IAlertObjectStorage alertStorage,
                                ObjectMapper objectMapper) {
        this.channelStorage = channelStorage;
        this.alertStorage = alertStorage;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/alerting/api/alert/notification/create")
    public ApiResponse<?> createProvider(@RequestBody Map<String, Object> request) throws JsonProcessingException {
        String name = (String) request.remove("name");
        Preconditions.checkIfTrue(name != null, "name property is missed.");

        if (channelStorage.exists(name)) {
            throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                            "The channel with name [%s] already exists",
                                            name);
        }

        Preconditions.checkIfTrue(request.containsKey("type"), "type property is missed.");

        String payload = objectMapper.writeValueAsString(request);
        objectMapper.readValue(payload, INotificationChannel.class);

        channelStorage.createChannel((String) request.get("type"),
                                     name,
                                     payload);
        return ApiResponse.success();
    }

    @PostMapping("/alerting/api/alert/notification/delete")
    public ApiResponse<?> deleteProvider(@RequestParam("name") String name) throws IOException {
        // Check if it's used
        List<AlertStorageObject> alerts = alertStorage.getAlertListByTime(new Timestamp(0), new Timestamp(System.currentTimeMillis()));
        for (AlertStorageObject alert : alerts) {
            List<String> notifications = this.objectMapper.readValue(alert.getPayload(), AlertRule.class).getNotifications();
            if (notifications.contains(name)) {
                return ApiResponse.fail(StringUtils.format("The notification channel can't be deleted because it's used by alert [%s].", alert.getAlertName()));
            }
        }

        channelStorage.deleteChannel(name);
        return ApiResponse.success();
    }

    @PostMapping("/alerting/api/alert/notification/get")
    public ApiResponse<?> getChannels() {
        return ApiResponse.success(
            this.channelStorage.getChannels(0)
                               .stream()
                               .map((obj) -> {
                                   try {
                                       Map<String, Object> props = objectMapper.readValue(obj.getPayload(), new TypeReference<TreeMap<String, Object>>() {
                                       });
                                       props.put("name", obj.getName());
                                       props.put("type", obj.getType());
                                       return props;
                                   } catch (JsonProcessingException e) {
                                       throw new RuntimeException(e);
                                   }
                               }));
    }
}
