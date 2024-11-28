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

package org.bithon.server.agent.controller.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.setting.ISettingReader;
import org.bithon.server.storage.setting.ISettingStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 16/5/24 6:01 pm
 */
@Slf4j
@Service
@ConditionalOnProperty(value = "bithon.agent-controller.enabled", havingValue = "true")
public class AgentSettingLoader implements SmartLifecycle {

    private final ScheduledExecutorService scheduledExecutorService;

    private final ISettingReader reader;
    private final ObjectMapper yamlFormatter;
    private final ObjectMapper jsonFormatter;
    private Map<String, List<ISettingReader.SettingEntry>> loadedSettings;

    public AgentSettingLoader(ISettingStorage storage,
                              ObjectMapper objectMapper) {
        this.scheduledExecutorService = new ScheduledThreadPoolExecutor(1, NamedThreadFactory.daemonThreadFactory("setting-loader"));
        this.reader = storage.createReader();
        this.jsonFormatter = objectMapper;
        this.yamlFormatter = new ObjectMapper(new YAMLFactory());

        this.loadedSettings = new ConcurrentHashMap<>();
    }

    /**
     * This bean is started by {@link AgentControllerServer}
     */
    @Override
    public boolean isAutoStartup() {
        return false;
    }

    @Override
    public void start() {
        this.load();

        scheduledExecutorService.scheduleWithFixedDelay(this::load, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public void stop() {
        if (this.isRunning()) {
            scheduledExecutorService.shutdown();
        }
    }

    @Override
    public boolean isRunning() {
        return !(scheduledExecutorService.isShutdown() || scheduledExecutorService.isTerminated());
    }

    public Map<String, String> get(String appName, String env) {
        List<ISettingReader.SettingEntry> settings = new ArrayList<>();

        settings.addAll(loadedSettings.getOrDefault(appName, Collections.emptyList()));
        settings.addAll(loadedSettings.getOrDefault(appName + "@" + env, Collections.emptyList()));

        return settings.stream()
                       .collect(Collectors.toMap(ISettingReader.SettingEntry::getName,
                                                 ISettingReader.SettingEntry::getValue));
    }

    public void update(String appName, String env) {
        List<ISettingReader.SettingEntry> settings;
        if (StringUtils.isBlank(env)) {
            settings = reader.getSettings(appName);
        } else {
            settings = reader.getSettings(appName, env);
        }
        this.loadedSettings.putAll(toSettingMap(settings));
    }

    private void load() {
        loadedSettings = new ConcurrentHashMap<>(toSettingMap(reader.getSettings()));
    }

    private Map<String, List<ISettingReader.SettingEntry>> toSettingMap(List<ISettingReader.SettingEntry> settings) {
        return settings.stream()
                       .peek((e) -> {
                           if (e.getFormat().equals("yaml")) {
                               try {
                                   e.setValue(convertYamlToJson(e.getValue()));
                                   e.setFormat("json");
                               } catch (JsonProcessingException ex) {
                                   log.error("Illegal format of setting", ex);
                               }
                           }
                       })
                       .collect(Collectors.groupingBy((e) -> {
                                                          if (StringUtils.isBlank(e.getEnvironment())) {
                                                              return e.getAppName();
                                                          } else {
                                                              return e.getAppName() + "@" + e.getEnvironment();
                                                          }
                                                      },
                                                      Collectors.mapping(v -> v, Collectors.toList())));
    }

    private String convertYamlToJson(String yaml) throws JsonProcessingException {
        return jsonFormatter.writeValueAsString(this.yamlFormatter.readTree(yaml));
    }
}
