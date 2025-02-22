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

package org.bithon.server.web.service.agent.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.agent.controller.config.AgentControllerConfig;
import org.bithon.server.agent.controller.config.PermissionConfig;
import org.bithon.server.agent.controller.rbac.Operation;
import org.bithon.server.commons.json.JsonPayloadFormatter;
import org.bithon.server.discovery.client.DiscoveredServiceInvoker;
import org.bithon.server.discovery.declaration.controller.IAgentControllerApi;
import org.bithon.server.storage.setting.ISettingReader;
import org.bithon.server.storage.setting.ISettingStorage;
import org.bithon.server.storage.setting.ISettingWriter;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Function;

/**
 * Manage agent configurations of target applications.
 * These configuration are stored in a persistent storage and can be retrieved by agents via the {@link IAgentController} service.
 *
 * @author Frank Chen
 * @date 26/1/24 1:03 pm
 */
@CrossOrigin
@RestController
@Conditional(WebServiceModuleEnabler.class)
public class AgentConfigurationApi {

    private final ISettingStorage settingStorage;
    private final AgentControllerConfig agentControllerConfig;
    private final IAgentControllerApi agentControllerApi;
    private final ObjectMapper objectMapper;

    public AgentConfigurationApi(ObjectMapper objectMapper,
                                 ISettingStorage settingStorage,
                                 AgentControllerConfig agentControllerConfig,
                                 DiscoveredServiceInvoker discoveredServiceInvoker) {
        this.objectMapper = objectMapper;
        this.settingStorage = settingStorage;
        this.agentControllerConfig = agentControllerConfig;
        this.agentControllerApi = discoveredServiceInvoker.createBroadcastApi(IAgentControllerApi.class);
    }

    @Data
    public static class GetRequest {
        @NotNull
        private String appName;

        /**
         * Optional
         */
        private String environment;

        /**
         * payload format. JSON or YAML
         */
        private String format = "default";
    }

    @PostMapping("/api/agent/configuration/get")
    public List<ISettingReader.SettingEntry> getConfiguration(@RequestBody GetRequest request) {
        // Transform payload to request format
        Function<List<ISettingReader.SettingEntry>, List<ISettingReader.SettingEntry>> formatTransformer =
            request.getFormat() == null || "default".equals(request.getFormat()) ?
            Function.identity() :
            settings -> {
                for (ISettingReader.SettingEntry entry : settings) {
                    if ("yaml".equals(request.getFormat())) {
                        // request format is YAML, convert JSON to YAML if necessary
                        if ("json".equals(entry.getFormat())) {
                            entry.setValue(JsonPayloadFormatter.YAML.format(entry.getValue(), objectMapper, null));
                            entry.setFormat("yaml");
                        }
                    } else {
                        // request format is JSON, convert YAML to JSON if necessary
                        if ("yaml".equals(entry.getFormat())) {
                            entry.setValue(JsonPayloadFormatter.JSON.format(entry.getValue(), objectMapper, null));
                            entry.setFormat("json");
                        }
                    }
                }
                return settings;
            };


        List<ISettingReader.SettingEntry> settings;
        if (StringUtils.isEmpty(request.getEnvironment())) {
            settings = settingStorage.createReader()
                                     .getSettings(request.getAppName().trim());
        } else {
            settings = settingStorage.createReader()
                                     .getSettings(request.getAppName().trim(),
                                                  request.getEnvironment().trim());

        }

        return formatTransformer.apply(settings);
    }

    @Data
    public static class AddRequest {
        @NotEmpty
        private String appName;

        private String environment;

        @NotEmpty
        private String name;

        @NotNull
        private String value;

        /**
         * can be NULL, or json/yaml
         * If it's NULL, it's default to json.
         */
        private String format;
    }

    /**
     * @param token Optional. If it's given, it's the token that's created by /api/security/token/create API
     *              If it's not given, current user is used for authorization
     */
    @PostMapping("/api/agent/configuration/add")
    public void addConfiguration(@RequestHeader(value = "token", required = false) String token,
                                 @Validated @RequestBody AddRequest request) {
        String format = request.getFormat() != null ? request.getFormat().trim() : "json";
        Preconditions.checkIfTrue("yaml".equals(format) || "json".equals(format),
                                  "Invalid format[%s] given. Only json or yaml is supported.",
                                  format);

        ObjectMapper mapper;
        if ("json".equals(format)) {
            mapper = new ObjectMapper();
        } else {
            mapper = new ObjectMapper(new YAMLFactory());
        }
        try {
            mapper.readTree(request.getValue());
        } catch (JsonProcessingException e) {
            throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                            "The 'value' is not valid format of %s",
                                            format);
        }

        String application = request.getAppName().trim();

        verifyPermission(application, token);

        String environment = request.getEnvironment() == null ? "" : request.getEnvironment().trim();
        String settingName = request.getName().trim();
        String settingVal = request.getValue().trim();
        boolean settingExists = settingStorage.createReader().isSettingExists(application, environment, settingName);
        Preconditions.checkIfTrue(!settingExists, "Setting already exist.");

        ISettingWriter writer = settingStorage.createWriter();
        writer.addSetting(application, environment, settingName, settingVal, format);

        // Notify agent controller about the change
        notifyConfigurationChange(request.appName, request.environment);
    }

    @PostMapping("/api/agent/configuration/update")
    public void updateConfiguration(@RequestHeader(value = "token", required = false) String token,
                                    @Validated @RequestBody AddRequest request) {
        String format = request.getFormat() != null ? request.getFormat().trim() : "json";
        Preconditions.checkIfTrue("yaml".equals(format) || "json".equals(format),
                                  "Invalid format[%s] given. Only json or yaml is supported.",
                                  format);

        ObjectMapper mapper;
        if ("json".equals(format)) {
            mapper = new ObjectMapper();
        } else {
            mapper = new ObjectMapper(new YAMLFactory());
        }
        try {
            mapper.readTree(request.getValue());
        } catch (JsonProcessingException e) {
            throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(),
                                            "The 'value' is not valid format of %s",
                                            format);
        }

        String application = request.getAppName().trim();
        verifyPermission(application, token);

        String environment = request.getEnvironment() == null ? "" : request.getEnvironment().trim();
        String settingName = request.getName().trim();
        String settingVal = request.getValue().trim();
        boolean settingExists = settingStorage.createReader().isSettingExists(application, environment, settingName);
        Preconditions.checkIfTrue(settingExists, "Setting does not exist.");

        ISettingWriter writer = settingStorage.createWriter();
        writer.updateSetting(application, environment, settingName, settingVal, format);

        // Notify agent controller about the change
        notifyConfigurationChange(request.appName, request.environment);
    }

    @Data
    public static class DeleteRequest {
        @NotEmpty
        private String appName;

        @NotEmpty
        private String name;

        private String environment;
    }

    @PostMapping("/api/agent/configuration/delete")
    public void deleteConfiguration(@RequestHeader(value = "token", required = false) String token,
                                    @Validated @RequestBody DeleteRequest request) {
        verifyPermission(request.getAppName(), token);

        ISettingWriter writer = settingStorage.createWriter();
        writer.deleteSetting(request.getAppName(),
                             request.getEnvironment() == null ? "" : request.getEnvironment().trim(),
                             request.getName());

        // Notify agent controller about the change
        notifyConfigurationChange(request.appName, request.environment);
    }

    private void notifyConfigurationChange(String appName, String env) {
        this.agentControllerApi.updateAgentSetting(appName, env);
    }

    private void verifyPermission(String application, String explicitGivenToken) {
        PermissionConfig permissionConfig = this.agentControllerConfig.getPermission();
        if (permissionConfig == null || !permissionConfig.isEnabled()) {
            return;
        }

        if (!StringUtils.isEmpty(explicitGivenToken)) {
            this.agentControllerConfig.getPermission()
                                      .verifyPermission(Operation.WRITE,
                                                        explicitGivenToken,
                                                        application,
                                                        "agent.setting");
        }

        Authentication authentication = SecurityContextHolder.getContext() == null
                                        ? null
                                        : SecurityContextHolder.getContext().getAuthentication();
        String user = authentication == null ? "anonymousUser" : (String) authentication.getPrincipal();
        this.agentControllerConfig.getPermission()
                                  .verifyPermission(Operation.WRITE,
                                                    user,
                                                    application,
                                                    "agent.setting");
    }
}
