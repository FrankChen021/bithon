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

package org.bithon.server.storage.jdbc.metric;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.server.metric.DataSourceSchema;
import org.bithon.server.metric.storage.IMetricCleaner;
import org.bithon.server.metric.storage.IMetricReader;
import org.bithon.server.metric.storage.IMetricStorage;
import org.bithon.server.metric.storage.IMetricWriter;
import org.jooq.CreateTableIndexStep;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;

import java.sql.Timestamp;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:37 下午
 */
@JsonTypeName("jdbc")
public class MetricJdbcStorage implements IMetricStorage {

    protected final DSLContext dslContext;

    @JsonCreator
    public MetricJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public IMetricWriter createMetricWriter(DataSourceSchema schema) {
        MetricTable table = new MetricTable(schema);
        initialize(schema, table);
        return new MetricJdbcWriter(dslContext, schema, table);
    }

    @Override
    public IMetricReader createMetricReader(DataSourceSchema schema) {
        ISqlExpressionFormatter sqlProvider;
        if (dslContext.dialect() == SQLDialect.H2) {
            sqlProvider = MetricJdbcReader.H2SqlExpressionFormatter.INSTANCE;
        } else {
            sqlProvider = MetricJdbcReader.DefaultSQLExpressionFormatter.INSTANCE;
        }
        return new MetricJdbcReader(dslContext, sqlProvider);
    }

    @SuppressWarnings("unchecked")
    @Override
    public IMetricCleaner createMetricCleaner(DataSourceSchema schema) {
        return timestamp -> {
            final MetricTable table = new MetricTable(schema);
            dslContext.deleteFrom(table).where(table.timestampField.lt(new Timestamp(timestamp))).execute();
        };
    }

    protected void initialize(DataSourceSchema schema, MetricTable table) {
        CreateTableIndexStep s = dslContext.createTableIfNotExists(table)
                                           .columns(table.fields())
                                           .index(table.getIndex(schema.isEnforceDuplicationCheck()));
        s.execute();
    }
}
