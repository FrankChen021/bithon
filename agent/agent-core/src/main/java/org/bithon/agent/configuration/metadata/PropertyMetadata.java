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

package org.bithon.agent.configuration.metadata;

import org.bithon.shaded.com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Metadata information for a configuration property.
 * This class is used both at compile-time for metadata generation
 * and at runtime for metadata consumption.
 *
 * @author frank.chen021@outlook.com
 */
public class PropertyMetadata {

    @JsonProperty("path")
    private String path;

    @JsonProperty("description")
    private String description;

    @JsonProperty("defaultValue")
    private String defaultValue;

    @JsonProperty("type")
    private String type;

    @JsonProperty("required")
    private boolean required;

    @JsonProperty("dynamic")
    private boolean dynamic;

    @JsonProperty("configurationClass")
    private String configurationClass;

    public PropertyMetadata() {
    }

    public PropertyMetadata(String path, String description, String defaultValue, String type, boolean required, boolean dynamic, String configurationClass) {
        this.path = path;
        this.description = description;
        this.defaultValue = defaultValue;
        this.type = type;
        this.required = required;
        this.dynamic = dynamic;
        this.configurationClass = configurationClass;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    public String getConfigurationClass() {
        return configurationClass;
    }

    public void setConfigurationClass(String configurationClass) {
        this.configurationClass = configurationClass;
    }

    @Override
    public String toString() {
        return "PropertyMetadata{" +
               "path='" + path + '\'' +
               ", description='" + description + '\'' +
               ", defaultValue='" + defaultValue + '\'' +
               ", type='" + type + '\'' +
               ", required=" + required +
               ", dynamic=" + dynamic +
               ", configurationClass='" + configurationClass + '\'' +
               '}';
    }
}
