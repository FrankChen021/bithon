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
import org.bithon.component.commons.expression.IExpressionInDepthVisitor;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.column.IColumn;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 22/4/25 10:05 pm
 */
class IdentifierExtractor implements IExpressionInDepthVisitor {

    private final ISchema schema;
    private final Set<String> identifiers = new LinkedHashSet<>();

    private IdentifierExtractor(ISchema schema) {
        this.schema = schema;
    }

    @Override
    public boolean visit(IdentifierExpression expression) {
        String field = expression.getIdentifier();
        IColumn columnSpec = schema.getColumnByName(field);
        if (columnSpec == null) {
            throw new RuntimeException(StringUtils.format("field [%s] can't be found in [%s].", field, schema.getName()));
        }
        identifiers.add(columnSpec.getName());

        return false;
    }

    public static Set<String> extractIdentifiers(ISchema schema, IExpression expression) {
        IdentifierExtractor extractor = new IdentifierExtractor(schema);
        expression.accept(extractor);
        return extractor.identifiers;
    }
}
