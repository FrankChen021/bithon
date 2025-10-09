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

package org.bithon.agent.rpc.brpc.cmd;

import org.bithon.component.brpc.BrpcService;
import org.bithon.component.brpc.message.serializer.Serializer;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/1/7 17:31
 */
@BrpcService(name = "agent.configuration", serializer = Serializer.JSON_SMILE)
public interface IConfigurationCommand {

    /**
     * get current loaded configuration from the agent for debug
     *
     * @param format JSON | YAML
     */
    List<String> getConfiguration(String format, boolean prettyFormat);

    class ConfigurationMetadata {
        private String path;
        private String type;
        private String description;

        /**
         * Serialized value based on the type
         */
        private String defaultValue;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }
    }

    List<ConfigurationMetadata> getConfigurationMetadata();
}
