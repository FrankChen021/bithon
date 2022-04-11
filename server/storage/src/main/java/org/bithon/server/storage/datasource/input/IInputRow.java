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

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/2 4:46 下午
 */
public interface IInputRow {

    Object getCol(String columnName);

    Long getColAsLong(String columnName);

    long getColAsLong(String columnName, long defaultValue);

    double getColAsDouble(String columnName, long defaultValue);

    String getColAsString(String columnName);

    <T> T getColAs(String columnName, Class<T> clazz);

    <T> T getCol(String columnName, T defaultValue);

    void updateColumn(String name, Object value);
}
