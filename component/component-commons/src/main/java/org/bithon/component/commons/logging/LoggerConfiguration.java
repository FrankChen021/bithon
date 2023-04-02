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

package org.bithon.component.commons.logging;

/**
 * This class is defined as pojo because it's used in RPC
 *
 * @author frank.chen021@outlook.com
 * @date 2023/4/1 20:18
 */
public class LoggerConfiguration {
    private String name;

    private LoggingLevel level;

    private LoggingLevel effectiveLevel;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LoggingLevel getLevel() {
        return level;
    }

    public void setLevel(LoggingLevel level) {
        this.level = level;
    }

    public LoggingLevel getEffectiveLevel() {
        return effectiveLevel;
    }

    public void setEffectiveLevel(LoggingLevel effectiveLevel) {
        this.effectiveLevel = effectiveLevel;
    }
}
