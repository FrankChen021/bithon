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

package org.bithon.server.storage.datasource.query.ast;

import lombok.Getter;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 16:11
 */
@Getter
public class Field implements IAST {
    private IAST field;
    private Name alias;

    public Field(IAST field) {
        this(field, (String) null);
    }

    public Field(IAST field, String alias) {
        this(field, alias == null ? null : new Alias(alias));
    }

    public Field(IAST field, Name alias) {
        this.field = field;
        this.alias = alias;
    }

    @Override
    public void accept(IASTVisitor visitor) {
        field.accept(visitor);
        if (alias != null) {
            alias.accept(visitor);
        }
    }
}
