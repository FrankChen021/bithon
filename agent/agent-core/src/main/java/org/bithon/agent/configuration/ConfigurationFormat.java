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

package org.bithon.agent.configuration;

import org.bithon.agent.instrumentation.expt.AgentException;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.shaded.com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import org.bithon.shaded.com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.bithon.shaded.com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

/**
 * @author Frank Chen
 * @date 19/2/24 2:08 pm
 */
public enum ConfigurationFormat {
    YAML {
        @Override
        public ObjectMapper createMapper() {
            return new ObjectMapper(new YAMLFactory()
                                        // Configure serialization format
                                        .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                                        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            );
        }
    },

    JSON {
        @Override
        public ObjectMapper createMapper() {
            return new ObjectMapper();
        }
    },

    PROPERTIES {
        @Override
        public ObjectMapper createMapper() {
            return new JavaPropsMapper();
        }
    };

    public abstract ObjectMapper createMapper();

    public static ConfigurationFormat determineFormatFromFile(String filePath) {
        if (filePath.endsWith(".yaml") || filePath.endsWith(".yml")) {
            return YAML;
        } else if (filePath.endsWith(".properties")) {
            return PROPERTIES;
        } else if (filePath.endsWith(".json")) {
            return JSON;
        } else {
            throw new AgentException("Unknown property file type: %s", filePath);
        }
    }

    public static ConfigurationFormat determineFormat(String format) {
        if ("yaml".equals(format) || "yml".equals(format)) {
            return ConfigurationFormat.YAML;
        } else if ("properties".equals(format)) {
            return ConfigurationFormat.PROPERTIES;
        } else if ("json".equals(format)) {
            return ConfigurationFormat.JSON;
        } else {
            throw new AgentException("Unknown format: %s", format);
        }
    }
}
