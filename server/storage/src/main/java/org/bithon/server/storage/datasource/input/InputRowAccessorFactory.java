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

package org.bithon.server.storage.datasource.input;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/26 22:09
 */
public class InputRowAccessorFactory {
    public static Function<IInputRow, Object> createGetter(String name) {
        int firstDotIndex = name.indexOf('.');
        if (firstDotIndex < 0) {
            return inputRow -> inputRow.getCol(name);
        }

        final String container = name.substring(0, firstDotIndex);
        final String nested = name.substring(firstDotIndex + 1);
        return inputRow -> {
            Object v = inputRow.getCol(container);
            if (v instanceof Map) {
                return ((Map<?, ?>) v).get(nested);
            }
            return null;
        };
    }

    public static BiConsumer<IInputRow, String> createSetter(String name) {
        int dotSeparatorIndex = name.indexOf('.');
        if (dotSeparatorIndex >= 0) {
            final String container = name.substring(0, dotSeparatorIndex);
            final String nested = name.substring(dotSeparatorIndex + 1);
            return (inputRow, val) -> {
                Object v = inputRow.getCol(container);
                if (v instanceof Map) {
                    ((Map<String, String>) v).put(nested, val);
                }
            };
        } else {
            return (inputRow, val) -> inputRow.updateColumn(name, val);
        }
    }
}
