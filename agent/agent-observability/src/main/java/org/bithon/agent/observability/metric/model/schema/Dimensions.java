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

package org.bithon.agent.observability.metric.model.schema;

import java.util.Arrays;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/2/5 20:24
 */
public final class Dimensions {
    private final String[] values;
    private volatile int hash;

    private Dimensions(String[] values) {
        this.values = values;
    }

    public int length() {
        return values.length;
    }

    public String getValue(int index) {
        return values[index];
    }

    public void setValue(int index, String value) {
        values[index] = value;
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            synchronized (this) {
                if (hash == 0) {
                    hash = Arrays.hashCode(values);
                }
            }
        }
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Dimensions)) {
            return false;
        }
        Dimensions that = (Dimensions) o;
        return this.hash == that.hash && Arrays.equals(this.values, that.values);
    }

    public static Dimensions of(String... values) {
        return new Dimensions(values);
    }
}
