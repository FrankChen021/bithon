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

package org.bithon.server.datasource.reader.jdbc.statement.ast;

import lombok.Getter;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.server.datasource.query.ast.IASTNode;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 15:04
 */
public class TableIdentifier implements IASTNode {

    @Getter
    private final IdentifierExpression identifier;

    public TableIdentifier(String identifier) {
        this.identifier = new IdentifierExpression(identifier);
    }

    @Override
    public String toString() {
        return identifier.toString();
    }
}
