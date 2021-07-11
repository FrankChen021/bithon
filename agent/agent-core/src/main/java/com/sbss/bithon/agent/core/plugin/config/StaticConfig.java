/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.core.plugin.config;

import com.sbss.bithon.agent.bootstrap.expt.AgentException;
import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import shaded.com.fasterxml.jackson.databind.DeserializationFeature;
import shaded.com.fasterxml.jackson.databind.JsonNode;
import shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/11 16:28
 */
public class StaticConfig {

    private final ObjectMapper mapper;
    private final JsonNode rootConfiguration;

    public StaticConfig(ObjectMapper mapper, JsonNode rootConfiguration) {
        this.mapper = mapper;
        this.rootConfiguration = rootConfiguration;
    }

    public static StaticConfig load(Class<? extends AbstractPlugin> pluginClass) {
        String pkgName = pluginClass.getPackage().getName().replace('.', '/');
        String name = pkgName + "/configuration.json";
        try (InputStream is = pluginClass.getClassLoader().getResourceAsStream(name)) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return new StaticConfig(mapper, mapper.readTree(is));
        } catch (IOException e) {
            throw new AgentException("Could not read configuration[%s]:%s", name, e.getMessage());
        }
    }

    public <T> Optional<T> getConfig(String names, Class<T> clazz) {
        JsonNode node = rootConfiguration;
        for (String name : names.split("\\.")) {
            node = node.get(name);
            if (node == null) {
                return Optional.empty();
            }
        }
        try {
            return Optional.of(mapper.treeToValue(node, clazz));
        } catch (IOException e) {
            throw new AgentException(e, "Could not get configuration[%s]:%s", names, e.getMessage());
        }
    }
}
