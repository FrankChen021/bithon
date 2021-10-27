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

package org.bithon.server.storage.jdbc.clickhouse;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.server.metric.DataSourceSchema;
import org.bithon.server.storage.jdbc.metric.MetricJdbcStorage;
import org.bithon.server.storage.jdbc.metric.MetricTable;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Index;
import org.jooq.SortField;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:37 下午
 */
@JsonTypeName("clickhouse")
public class MetricStorage extends MetricJdbcStorage {

    @JsonCreator
    public MetricStorage(@JacksonInject(useInput = OptBoolean.FALSE) DSLContext dslContext) {
        super(dslContext);
    }

    @Override
    protected void initialize(DataSourceSchema schema, MetricTable table) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("CREATE TABLE IF NOT EXISTS `%s` (\n", table.getName()));
        for (Field<?> f : table.fields()) {

            if (f.getDataType().hasPrecision()) {
                sb.append(String.format("`%s` %s(%d, %d) ,\n",
                                        f.getName(),
                                        f.getDataType().getTypeName(),
                                        f.getDataType().precision(),
                                        f.getDataType().scale()));
            } else {
                sb.append(String.format("`%s` %s ,\n", f.getName(), f.getDataType().getTypeName()));
            }
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append(") ENGINE=MergeTree ORDER BY(");
        Index idx = table.getIndex(schema.isEnforceDuplicationCheck());
        for (SortField<?> f : idx.getFields()) {
            sb.append(String.format("`%s`,", f.getName()));
        }
        sb.delete(sb.length() - 1, sb.length());
        sb.append(");");
        dslContext.execute(sb.toString());
    }
}
