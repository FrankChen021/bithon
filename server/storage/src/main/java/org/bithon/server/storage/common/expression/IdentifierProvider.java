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

import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.validation.ExpressionValidationException;
import org.bithon.component.commons.expression.validation.IIdentifierProvider;
import org.bithon.component.commons.expression.validation.Identifier;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.column.IColumn;

/**
 * @author Frank Chen
 * @date 21/1/24 11:45 am
 */
class IdentifierProvider implements IIdentifierProvider {
    private final DataSourceSchema schema;

    IdentifierProvider(DataSourceSchema schema) {
        this.schema = schema;
    }

    @Override
    public Identifier getIdentifier(String identifier) {
        IColumn column = schema.getColumnByName(identifier);
        if (column == null) {
            // A special and ugly check.
            // For indexed tags filter, when querying the dimensions, we need to convert its alias to its field name.
            // However, when searching spans with tag filters, the schema here does not contain the tags.
            // We need to ignore this case.
            // The ignored tags will be processed later in the trace module.
            if (identifier.startsWith("tags.")) {
                return new Identifier(identifier, IDataType.STRING);
            }

            throw new ExpressionValidationException("Identifier [%s] not defined in the data source [%s].",
                                                    identifier,
                                                    schema.getName());
        }

        // Change to raw name and correct type
        return new Identifier(column.getName(), column.getDataType());
    }
}
