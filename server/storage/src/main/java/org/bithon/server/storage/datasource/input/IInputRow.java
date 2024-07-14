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

    default Long getColAsLong(String columnName) {
        Object val = getCol(columnName);
        if (val instanceof Number) {
            return ((Number) val).longValue();
        } else {
            return 0L;
        }
    }

    default long getColAsLong(String columnName, long defaultValue) {
        Number number = getColAs(columnName, Number.class);
        return number == null ? defaultValue : number.longValue();
    }

    default double getColAsDouble(String columnName, long defaultValue) {
        Number number = getColAs(columnName, Number.class);
        return number == null ? defaultValue : number.doubleValue();
    }

    default String getColAsString(String columnName) {
        return getColAs(columnName, String.class);
    }

    default <T> T getColAs(String columnName, Class<T> clazz) {
        //noinspection unchecked
        return (T) getCol(columnName);
    }

    default <T> T getCol(String columnName, T defaultValue) {
        // when columnName exist but its value is null, the returned obj above is NOT null
        // So, additional check is needed to return correct default value
        Object val = getCol(columnName);
        //noinspection unchecked
        return val == null ? defaultValue : (T) val;
    }

    void updateColumn(String name, Object value);

    Map<String, Object> toMap();
}
