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

package org.bithon.server.collector.ctrl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.bithon.agent.rpc.brpc.BrpcMessageHeader;
import org.bithon.agent.rpc.brpc.setting.ISettingFetcher;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.component.commons.utils.SupplierUtils;
import org.bithon.server.storage.setting.ISettingReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/6/30 3:34 下午
 */
@Slf4j
public class AgentSettingFetcher implements ISettingFetcher {

    private final ISettingReader reader;
    private final Supplier<ObjectMapper> yamlFormatter;
    private final ObjectMapper jsonFormatter;

    public AgentSettingFetcher(ISettingReader reader, ObjectMapper objectMapper) {
        this.reader = reader;
        this.jsonFormatter = objectMapper;
        this.yamlFormatter = SupplierUtils.cachedWithLock(() -> new ObjectMapper(new YAMLFactory()));
    }

    @Override
    public Map<String, String> fetch(BrpcMessageHeader header, long lastModifiedSince) {
        // Always fetch all configuration by setting 'since' parameter to 0
        return getConfiguration(header.getAppName(), header.getEnv(), 0);
    }

    private Map<String, String> getConfiguration(String appName, String env, long since) {
        if (StringUtils.hasText(env)) {
            appName += "-" + env;
        }

        List<ISettingReader.SettingEntry> settings = reader.getSettings(appName, since);

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
        return jsonFormatter.writeValueAsString(this.yamlFormatter.get().readTree(yaml));
    }
}
