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

package org.bithon.server.storage.common.expression;

import org.bithon.component.commons.expression.validation.ExpressionValidationException;
import org.bithon.component.commons.expression.validation.IIdentifier;
import org.bithon.component.commons.expression.validation.IIdentifierProvider;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.column.IColumn;

/**
 * @author Frank Chen
 * @date 21/1/24 11:45 am
 */
class IdentifierProvider implements IIdentifierProvider {
    private final ISchema schema;

    IdentifierProvider(ISchema schema) {
        this.schema = schema;
    }

    @Override
    public IIdentifier getIdentifier(String identifier) {
        IColumn column = schema.getColumnByName(identifier);
        if (column == null) {
            throw new ExpressionValidationException("Identifier [%s] not defined in schema [%s]",
                                                    identifier,
                                                    schema.getName());
        }

        return column;
    }
}
