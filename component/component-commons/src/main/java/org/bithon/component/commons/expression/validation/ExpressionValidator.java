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

package org.bithon.component.commons.expression.validation;

import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.IdentifierExpression;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/1/20 14:00
 */
public class ExpressionValidator {

    private final IIdentifierProvider identifierProvider;

    public ExpressionValidator(IIdentifierProvider identifierProvider) {
        this.identifierProvider = identifierProvider;
    }

    public void validate(IExpression expression) {
        // Validate all identifier expressions recursively
        expression.accept(new IdentifierExpressionValidator(identifierProvider));

        // Type validation
        expression.accept(new ExpressionTypeValidator());
    }

    public static class IdentifierExpressionValidator implements IExpressionVisitor {
        final IIdentifierProvider provider;

        public IdentifierExpressionValidator(IIdentifierProvider provider) {
            this.provider = provider;
        }

        @Override
        public boolean visit(IdentifierExpression expression) {
            Identifier dataType = provider.getIdentifier(expression.getIdentifier());
            // Change to raw name and correct type
            expression.setIdentifier(dataType.getName());
            expression.setDataType(dataType.getDataType());
            return true;
        }
    }
}
