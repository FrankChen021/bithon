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

import org.bithon.component.commons.utils.Preconditions;

/**
 * This class is defined as pojo because it's used in RPC
 *
 * @author frank.chen021@outlook.com
 * @date 2023/4/1 20:18
 */
public class LoggerConfiguration {
    public String name;
    public LoggingLevel level;
    public LoggingLevel effectiveLevel;

    public LoggerConfiguration() {
    }

    public LoggerConfiguration(String name, LoggingLevel level, LoggingLevel effectiveLevel) {
        this.name = name;
        this.level = level;
        this.effectiveLevel = effectiveLevel;
    }

    public Object[] toObjects() {
        return new Object[]{name, level, effectiveLevel};
    }

    public static class Comparator implements java.util.Comparator<LoggerConfiguration> {
        public static final Comparator INSTANCE = new Comparator("ROOT");

        private final String rootLoggerName;

        /**
         * Create a new {@link Comparator} instance.
         *
         * @param rootLoggerName the name of the "root" logger
         */
        public Comparator(String rootLoggerName) {
            Preconditions.checkNotNull(rootLoggerName, "RootLoggerName must not be null");
            this.rootLoggerName = rootLoggerName;
        }

        @Override
        public int compare(LoggerConfiguration lhs, LoggerConfiguration rhs) {
            if (this.rootLoggerName.equals(lhs.name)) {
                return -1;
            }
            if (this.rootLoggerName.equals(rhs.name)) {
                return 1;
            }
            return lhs.name.compareTo(rhs.name);
        }

    }
}
