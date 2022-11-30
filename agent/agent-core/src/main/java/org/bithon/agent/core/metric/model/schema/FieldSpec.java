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

package org.bithon.agent.core.metric.model.schema;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/30 22:57
 */
public class FieldSpec {
    public static final int TYPE_STRING = 0;
    public static final int TYPE_LONG = 1;
    public static final int TYPE_DOUBLE = 2;
    public static final int TYPE_HISTOGRAM = 3;

    private final String name;
    private final int type;

    public static FieldSpec of(String name, int type) {
        return new FieldSpec(name, type);
    }

    public FieldSpec(String name, int type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public int getType() {
        return type;
    }
}
