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
public class ResultColumn implements IAST {
    private final IAST columnExpression;
    private final Alias alias;

    public ResultColumn(String name) {
        this(new Column(name), (Alias) null);
    }

    ResultColumn(IAST columnExpression) {
        this(columnExpression, (String) null);
    }

    public ResultColumn(IAST columnExpression, String alias) {
        this(columnExpression, alias == null ? null : new Alias(alias));
    }

    public ResultColumn(IAST columnExpression, Alias alias) {
        this.columnExpression = columnExpression;
        this.alias = alias;
    }

    @Override
    public void accept(IASTVisitor visitor) {
        columnExpression.accept(visitor);
        if (alias != null) {
            alias.accept(visitor);
        }
    }
}
