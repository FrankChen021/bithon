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

/**
 * Metadata information for a configuration property.
 * This class is used both at compile-time for metadata generation
 * and at runtime for metadata consumption.
 *
 * @author frank.chen021@outlook.com
 */
public class PropertyMetadata {
    public String path;
    public String description;
    public String suggestion;
    public String defaultValue;
    public String type;
    public boolean required;
    public boolean dynamic;
    public String configurationClass;
}
