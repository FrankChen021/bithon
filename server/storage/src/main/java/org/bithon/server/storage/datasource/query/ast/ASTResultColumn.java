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
import org.bithon.component.commons.utils.StringUtils;

/**
 * Take SQL as example, this AST nodes represent a column that appears right after SELECT keyword
 *
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 16:11
 */
@Getter
public class ASTResultColumn implements IASTNode {
    private final IASTNode columnExpression;
    private final ASTColumnAlias alias;

    public ASTResultColumn(String name) {
        this(new ASTColumn(name), (ASTColumnAlias) null);
    }

    public ASTResultColumn(String name, String alias) {
        this(new ASTColumn(name), alias == null ? null : new ASTColumnAlias(alias));
    }

    ASTResultColumn(IASTNode columnExpression) {
        this(columnExpression, (String) null);
    }

    public ASTResultColumn(IASTNode columnExpression, String alias) {
        this(columnExpression, alias == null ? null : new ASTColumnAlias(alias));
    }

    public ASTResultColumn(IASTNode columnExpression, ASTColumnAlias alias) {
        this.columnExpression = columnExpression;
        this.alias = alias;
    }

    public ASTResultColumn withAlias(String alias) {
        if (this.alias != null && this.alias.getName().equals(alias)) {
            return this;
        }
        return new ASTResultColumn(this.columnExpression, alias);
    }

    public String getResultColumnName() {
        if (alias != null) {
            return alias.getName();
        }
        if (columnExpression instanceof ASTColumn) {
            return ((ASTColumn) columnExpression).getName();
        }
        throw new RuntimeException(StringUtils.format("no result name for result column [%s]", columnExpression));
    }

    @Override
    public void accept(IASTNodeVisitor visitor) {
        columnExpression.accept(visitor);
        if (alias != null) {
            alias.accept(visitor);
        }
    }
}
