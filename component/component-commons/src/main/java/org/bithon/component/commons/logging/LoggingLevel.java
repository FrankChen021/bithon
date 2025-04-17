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
 * @author frank.chen021@outlook.com
 * @date 2023/4/1 20:19
 */
public enum LoggingLevel {
    OFF(0),
    TRACE(1),
    DEBUG(2),
    INFO(3),
    WARN(4),
    ERROR(5),
    FATAL(6);

    private final int intLevel;

    LoggingLevel(int intLevel) {
        this.intLevel = intLevel;
    }

    public int intLevel() {
        return intLevel;
    }
}
