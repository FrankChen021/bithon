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

package org.bithon.server.meta;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 9:50 上午
 */
public class Metadata {

    @Getter
    private final String name;
    @Getter
    private final String type;
    @Getter
    private final Long parent;
    @Getter
    @Setter
    private Long id;

    public Metadata(String name, String type, Long parent) {
        this.name = name;
        this.type = type;
        this.parent = parent;
    }

    @Override
    public boolean equals(Object rhs) {
        if (rhs instanceof Metadata) {
            Metadata that = ((Metadata) rhs);
            return this.name.equals(that.name)
                   && this.type.equals(that.type)
                   && this.parent.equals(that.parent);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, type, parent);
    }
}
