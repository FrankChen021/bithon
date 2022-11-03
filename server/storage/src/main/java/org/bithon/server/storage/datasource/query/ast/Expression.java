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
import org.bithon.server.storage.datasource.query.parser.FieldExpressionParserImpl;
import org.bithon.server.storage.datasource.query.parser.FieldExpressionVisitorAdaptor;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/3 22:18
 */
public class Expression implements IAST {

    @Getter
    private final String expression;

    public Expression(String expression) {
        this.expression = expression;
    }

    public void visitExpression(FieldExpressionVisitorAdaptor visitorAdaptor) {
        FieldExpressionParserImpl parser = FieldExpressionParserImpl.create(this.expression);
        parser.visit(visitorAdaptor);
    }

    @Override
    public void accept(IASTVisitor visitor) {
        visitor.visit(this);
    }
}
