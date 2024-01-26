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

package org.bithon.server.collector.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.setting.ISettingReader;
import org.bithon.server.storage.setting.ISettingStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 7:37 下午
 */
@Slf4j
@Service
@ConditionalOnProperty(value = "collector-brpc.enabled", havingValue = "true")
public class AgentConfigurationService {

    private final ISettingReader settingReader;

    public AgentConfigurationService(ISettingStorage storage) {
        this.settingReader = storage.createReader();
    }

    public Map<String, String> getConfiguration(String appName, String env, long since) {
        if (StringUtils.hasText(env)) {
            appName += "-" + env;
        }

        List<ISettingReader.SettingEntry> settings = settingReader.getSettings(appName, since);

        Map<String, String> map = new HashMap<>();
        for (ISettingReader.SettingEntry record : settings) {
            String value = record.getValue();
            if (record.getFormat().equals("yaml")) {
                try {
                    value = convertYamlToJson(value);
                } catch (JsonProcessingException e) {
                    log.error("Illegal format of setting", e);
                }
            }
            map.put(record.getName(), value);
        }
        return map;
    }

    private String convertYamlToJson(String yaml) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(new ObjectMapper(new YAMLFactory()).readTree(yaml));
    }
}
