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

package org.bithon.server.storage.jdbc.common.statement.builder;


import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.optimzer.AbstractOptimizer;
import org.bithon.server.storage.datasource.ISchema;

/**
 * @author frank.chen021@outlook.com
 * @date 22/4/25 10:06 pm
 */
class QualifiedIdentifierTransformer extends AbstractOptimizer {
    private final String qualifier;

    public QualifiedIdentifierTransformer(ISchema schema) {
        this.qualifier = schema.getDataStoreSpec().getStore();
    }

    @Override
    public IExpression visit(IdentifierExpression expression) {
        expression.setQualifier(qualifier);
        return expression;
    }
}
