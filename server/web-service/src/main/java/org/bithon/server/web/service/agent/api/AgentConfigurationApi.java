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
import lombok.Data;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.collector.ctrl.config.AgentControllerConfig;
import org.bithon.server.storage.setting.ISettingReader;
import org.bithon.server.storage.setting.ISettingStorage;
import org.bithon.server.storage.setting.ISettingWriter;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Frank Chen
 * @date 26/1/24 1:03 pm
 */
@CrossOrigin
@RestController
@Conditional(WebServiceModuleEnabler.class)
public class AgentConfigurationApi {

    private final ISettingStorage settingStorage;
    private final ObjectMapper objectMapper;
    private final AgentControllerConfig agentControllerConfig;

    public AgentConfigurationApi(ISettingStorage settingStorage,
                                 ObjectMapper objectMapper,
                                 AgentControllerConfig agentControllerConfig) {
        this.settingStorage = settingStorage;
        this.objectMapper = objectMapper;
        this.agentControllerConfig = agentControllerConfig;
    }

    @Data
    public static class GetRequest {
        @NotNull
        private String appName;

        private String format = "default";
    }

    @PostMapping("/api/agent/configuration/get")
    public List<ISettingReader.SettingEntry> getConfiguration(@RequestBody GetRequest request) {
        return settingStorage.createReader()
                             .getSettings(request.getAppName(), 0);
    }

    @Data
    public static class AddRequest {
        @NotNull
        private String appName;

        @NotNull
        private String name;

        @NotNull
        private String value;

        /**
         * can be NULL, or json/yaml
         * If it's NULL, it's default to json.
         */
        private String format;
    }

    @PostMapping("/api/agent/configuration/add")
    public void addConfiguration(@RequestHeader("token") String token,
                                 @Validated @RequestBody AddRequest request) {
        String format = request.getFormat() != null ? request.getFormat() : "json";
        Preconditions.checkIfTrue("yaml".equals(request.getFormat()) || "json".equals(request.getFormat()),
                                  "Invalid format[%s] given. Only json or yaml is supported.",
                                  format);

        ObjectMapper mapper;
        if (format.equals("json")) {
            mapper = new ObjectMapper();
        } else {
            mapper = new ObjectMapper(new YAMLFactory());
        }
        try {
            mapper.readTree(request.getValue());
        } catch (JsonProcessingException e) {
            throw new HttpMappableException(HttpStatus.BAD_REQUEST.value(), "The 'value' is not valid format of %s", format);
        }

        this.agentControllerConfig.getPermission().verifyPermission(objectMapper,
                                                                    request.getAppName(),
                                                                    token);

        ISettingWriter writer = settingStorage.createWriter();
        writer.addSetting(request.getAppName(),
                          request.getName(),
                          request.getValue(),
                          request.getFormat());
    }

    @Data
    public static class DeleteRequest {
        @NotNull
        private String appName;

        @NotNull
        private String name;
    }

    @PostMapping("/api/agent/configuration/delete")
    public void deleteConfiguration(@RequestHeader("token") String token, @Validated @RequestBody DeleteRequest request) {
        this.agentControllerConfig.getPermission().verifyPermission(objectMapper,
                                                                    request.getAppName(),
                                                                    token);

        ISettingWriter writer = settingStorage.createWriter();
        writer.deleteSetting(request.getAppName(),
                             request.getName());
    }
}
