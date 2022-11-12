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

import java.util.HashMap;

/**
 * An implementation for object deserialized from JSON.
 * Should not use this class directly.
 *
 * @author frank.chen021@outlook.com
 * @date 2022/11/12 11:51
 */
public class InputRowImpl extends HashMap<String, Object> implements IInputRow {
    @Override
    public Object getCol(String columnName) {
        return this.get(columnName);
    }

    @Override
    public void updateColumn(String name, Object value) {
        this.put(name, value);
    }
}
