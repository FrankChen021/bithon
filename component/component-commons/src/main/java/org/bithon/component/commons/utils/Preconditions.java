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

package org.bithon.component.commons.utils;

/**
 * @author Frank Chen
 * @date 23/3/22 2:38 PM
 */
public class Preconditions {
    public static <T> T checkArgumentNotNull(String name, T value) {
        if (value == null) {
            throw new InvalidValueException("parameter [%s] should not be NULL", name);
        }
        return value;
    }

    public static <T> T checkNotNull(T value, String message) {
        if (value == null) {
            throw new InvalidValueException(message);
        }
        return value;
    }

    public static <T> T checkNotNull(T value, String messageFormat, Object... args) {
        if (value == null) {
            throw new InvalidValueException(messageFormat, args);
        }
        return value;
    }

    public static void checkIf(boolean expression, String message) {
        if (!expression) {
            throw new InvalidValueException(message);
        }
    }

    public static void checkIf(boolean expression, String messageFormat, Object... args) {
        if (!expression) {
            throw new InvalidValueException(messageFormat, args);
        }
    }

    public static class InvalidValueException extends RuntimeException {
        public InvalidValueException(String message) {
            super(message);
        }

        public InvalidValueException(String format, Object... args) {
            super(StringUtils.format(format, args));
        }
    }
}
