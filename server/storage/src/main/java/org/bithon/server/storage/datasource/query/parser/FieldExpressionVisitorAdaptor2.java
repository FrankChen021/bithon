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

package org.bithon.server.storage.datasource.query.parser;


import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.column.IColumn;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/23
 */
public abstract class FieldExpressionVisitorAdaptor2 implements FieldExpressionVisitorAdaptor {
    public final void visitField(String field) {
        IColumn columnSpec = getSchema().getColumnByName(field);
        if (columnSpec == null) {
            throw new RuntimeException(StringUtils.format("field [%s] can't be found in [%s].", field, getSchema().getName()));
        }
        visitField(columnSpec);
    }

    public abstract void visitField(IColumn columnSpec);

    protected abstract DataSourceSchema getSchema();
}
