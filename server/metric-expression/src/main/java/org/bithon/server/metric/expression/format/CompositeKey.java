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

package org.bithon.server.metric.expression.format;


import java.util.Arrays;

/**
 * @author frank.chen021@outlook.com
 * @date 7/4/25 10:17 am
 */
public class CompositeKey {
    private final Object[] parts;

    public CompositeKey(Object[] parts) {
        this.parts = parts;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CompositeKey)) {
            return false;
        }
        if (getClass() != other.getClass()) {
            return false;
        }
        return Arrays.equals(parts, ((CompositeKey) other).parts);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(parts);
    }
}

