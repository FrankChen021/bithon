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

package org.bithon.server.web.service.agent.sql.table;

import org.apache.calcite.DataContext;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Linq4j;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.ScannableTable;
import org.apache.calcite.schema.impl.AbstractTable;
import org.bithon.server.web.service.common.calcilte.SqlExecutionContext;

import java.util.List;

/**
 * @author Frank Chen
 * @date 1/3/23 8:18 pm
 */
abstract class AbstractBaseTable extends AbstractTable implements ScannableTable {
    protected RelDataType rowType;

    protected abstract Class<?> getRecordClazz();

    @Override
    public RelDataType getRowType(final RelDataTypeFactory typeFactory) {
        if (rowType == null) {
            rowType = typeFactory.createJavaType(getRecordClazz());
        }
        return rowType;
    }

    @Override
    public Enumerable<Object[]> scan(final DataContext root) {
        return Linq4j.asEnumerable(getData((SqlExecutionContext) root));
    }

    protected abstract List<Object[]> getData(SqlExecutionContext executionContext);
}
