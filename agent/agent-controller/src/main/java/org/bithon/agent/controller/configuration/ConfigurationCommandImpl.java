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

package org.bithon.agent.controller.configuration;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.configuration.metadata.PropertyMetadata;
import org.bithon.agent.controller.cmd.IAgentCommand;
import org.bithon.agent.instrumentation.logging.LoggerFactory;
import org.bithon.agent.rpc.brpc.cmd.IConfigurationCommand;
import org.bithon.shaded.com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Implementation of configuration command that provides access to agent configuration
 * and metadata. Uses compile-time generated metadata for better performance and accuracy.
 *
 * @author frank.chen021@outlook.com
 * @date 2023/1/7 17:33
 */
public class ConfigurationCommandImpl implements IConfigurationCommand, IAgentCommand {

    @Override
    public List<String> getConfiguration(String format, boolean prettyFormat) {
        return Collections.singletonList(ConfigurationManager.getInstance()
                                                             .getActiveConfiguration(format.toLowerCase(Locale.ENGLISH), prettyFormat));
    }

    @Override
    public List<IConfigurationCommand.ConfigurationMetadata> getConfigurationProperties() {
        try {
            // Use the new compile-time generated metadata
            List<PropertyMetadata> properties = ConfigurationPropertyLoader.loadAllProperties();

            return properties.stream()
                             .map(property -> {
                                 IConfigurationCommand.ConfigurationMetadata metadata = new IConfigurationCommand.ConfigurationMetadata();

                                 metadata.setPath(property.path);
                                 metadata.setType(property.type);
                                 metadata.setDescription(property.description);
                                 metadata.setRequired(property.required);
                                 metadata.setDynamic(property.dynamic);
                                 metadata.setDefaultValue(property.defaultValue);
                                 metadata.setValue(getRuntimeValue(property.path)); // Use the runtime value from ConfigurationManager
                                 metadata.setChanged(isValueChanged(property.defaultValue, metadata.getValue()));

                                 return metadata;
                             })
                             .collect(Collectors.toList());
        } catch (Exception e) {
            // Log the error and return empty list
            LoggerFactory.getLogger(ConfigurationCommandImpl.class)
                         .warn("Failed to load configuration metadata: " + e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Get the runtime value for a property path from the configuration manager.
     *
     * @param propertyPath the property path to look up
     * @return the runtime value as a string, or null if not found
     */
    private String getRuntimeValue(String propertyPath) {
        try {
            JsonNode value = ConfigurationManager.getInstance().getPropertyValue(propertyPath);
            if (value == null || value.isNull()) {
                return null;
            }

            if (value.isTextual()) {
                return value.asText();
            } else if (value.isNumber()) {
                return value.asText();
            } else if (value.isBoolean()) {
                return String.valueOf(value.asBoolean());
            } else if (value.isArray() || value.isObject()) {
                // For complex types, serialize to JSON string
                return value.toString();
            } else {
                return value.asText();
            }
        } catch (Exception e) {
            return "Failed to get runtime value:" + e.getMessage();
        }
    }

    /**
     * Determine if the runtime value differs from the default value.
     *
     * @param defaultValue the default value from metadata
     * @param runtimeValue the current runtime value
     * @return true if the values are different, false otherwise
     */
    private boolean isValueChanged(String defaultValue, String runtimeValue) {
        // Both null - no change
        if (defaultValue == null && runtimeValue == null) {
            return false;
        }

        // One is null, the other is not - changed
        if (defaultValue == null || runtimeValue == null) {
            return true;
        }

        // Compare string values
        return !defaultValue.equals(runtimeValue);
    }
}
