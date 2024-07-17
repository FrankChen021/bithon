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

import org.bithon.component.commons.expression.IEvaluationContext;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/2 4:46 下午
 */
public interface IInputRow extends IEvaluationContext {

    default Object get(String name) {
        return getCol(name);
    }

    Object getCol(String columnName);

    default long getColAsLong(String columnName) {
        return getColAsLong(columnName, 0);
    }

    default long getColAsLong(String columnName, long defaultValue) {
        Object number = getCol(columnName);
        if (number == null) {
            return defaultValue;
        }
        if (number instanceof Number) {
            return ((Number) number).longValue();
        }
        if (number instanceof String) {
            try {
                return Long.parseLong((String) number);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    default double getColAsDouble(String columnName, long defaultValue) {
        Object number = getCol(columnName);
        if (number == null) {
            return defaultValue;
        }
        if (number instanceof Number) {
            return ((Number) number).doubleValue();
        }
        if (number instanceof String) {
            try {
                return Double.parseDouble((String) number);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    default String getColAsString(String columnName) {
        Object val = getCol(columnName);
        return val instanceof String ? (String) val : val == null ? null : val.toString();
    }

    default String getColAsString(String columnName, String defaultValue) {
        Object val = getCol(columnName);
        return val == null ? defaultValue : val.toString();
    }

    void updateColumn(String name, Object value);

    Map<String, Object> toMap();
}
