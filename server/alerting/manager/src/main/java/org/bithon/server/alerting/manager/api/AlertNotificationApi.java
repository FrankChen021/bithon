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
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.alerting.common.model.Alert;
import org.bithon.server.alerting.manager.ManagerModuleEnabler;
import org.bithon.server.alerting.manager.api.parameter.ApiResponse;
import org.bithon.server.alerting.notification.provider.INotificationProvider;
import org.bithon.server.storage.alerting.IAlertNotificationProviderStorage;
import org.bithon.server.storage.alerting.IAlertObjectStorage;
import org.bithon.server.storage.alerting.pojo.AlertStorageObject;
import org.springframework.context.annotation.Conditional;
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

    private final IAlertNotificationProviderStorage storage;
    private final IAlertObjectStorage alertStorage;
    private final ObjectMapper objectMapper;

    public AlertNotificationApi(IAlertNotificationProviderStorage storage,
                                IAlertObjectStorage alertStorage,
                                ObjectMapper objectMapper) {
        this.storage = storage;
        this.alertStorage = alertStorage;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/alerting/api/alert/notification/create")
    public ApiResponse createProvider(@RequestBody Map<String, Object> request) throws JsonProcessingException {
        String id = (String) request.remove("id");
        String name = (String) request.remove("name");

        Preconditions.checkIfTrue(id != null, "id property is missed.");
        Preconditions.checkIfTrue(name != null, "name property is missed.");
        Preconditions.checkIfTrue(request.containsKey("type"), "type property is missed.");

        String s = objectMapper.writeValueAsString(request);
        objectMapper.readValue(s, INotificationProvider.class);

        storage.creatProvider(id,
                              name,
                              (String) request.get("type"),
                              s);
        return ApiResponse.success();
    }

    @PostMapping("/alerting/api/alert/notification/delete")
    public ApiResponse deleteProvider(@RequestParam("id") String id) throws IOException {
        // Check if it's used
        List<AlertStorageObject> alerts = alertStorage.getAlertListByTime(new Timestamp(0), new Timestamp(System.currentTimeMillis()));
        for (AlertStorageObject alert : alerts) {
            List<String> notifications = this.objectMapper.readValue(alert.getPayload(), Alert.class).getNotifications();
            if (notifications.contains(id)) {
                return ApiResponse.fail(StringUtils.format("The notification can't be deleted because it's used by alert [%s].", alert.getAlertName()));
            }
        }

        storage.deleteProvider(id);
        return ApiResponse.success();
    }

    @PostMapping("/alerting/api/alert/notification/get")
    public ApiResponse getProviders() {
        return ApiResponse.success(
            this.storage.loadProviders(0)
                        .stream()
                        .map((obj) -> {
                            try {
                                Map<String, Object> props = objectMapper.readValue(obj.getPayload(), new TypeReference<TreeMap<String, Object>>() {
                                });
                                props.put("id", obj.getProviderId());
                                props.put("name", obj.getName());
                                props.put("type", obj.getType());
                                return props;
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        }));
    }
}
