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

package org.bithon.server.datasource.query.plan.physical;


import java.util.Arrays;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 7/4/25 10:17 am
 */
public record CompositeKey(Object[] keys, int precomputedHashCode) {

    public CompositeKey(Object[] keys) {
        this(keys, Arrays.hashCode(keys));
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CompositeKey)) {
            return false;
        }
        return precomputedHashCode == ((CompositeKey) other).precomputedHashCode &&
               Arrays.equals(keys, ((CompositeKey) other).keys);
    }

    @Override
    public int hashCode() {
        return precomputedHashCode;
    }

    public static CompositeKey from(List<Column> columns, int row) {
        Object[] keys = new Object[columns.size()];
        for (int i = 0, size = columns.size(); i < size; i++) {
            keys[i] = columns.get(i).getObject(row);
        }
        return new CompositeKey(keys);
    }
}
