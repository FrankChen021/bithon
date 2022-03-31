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
import org.bithon.server.storage.metrics.IMetricCleaner;
import org.bithon.server.storage.metrics.IMetricReader;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.metrics.IMetricWriter;
import org.jooq.CreateTableIndexStep;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;

import java.sql.Timestamp;

import static org.bithon.server.storage.jdbc.JdbcStorageAutoConfiguration.BITHON_JDBC_DSL;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:37 下午
 */
@JsonTypeName("jdbc")
public class MetricJdbcStorage implements IMetricStorage {

    protected final DSLContext dslContext;

    @JsonCreator
    public MetricJdbcStorage(@JacksonInject(value = BITHON_JDBC_DSL, useInput = OptBoolean.FALSE) DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public IMetricWriter createMetricWriter(DataSourceSchema schema) {
        MetricTable table = new MetricTable(schema);
        initialize(schema, table);
        return new MetricJdbcWriter(dslContext, table);
    }

    @Override
    public IMetricReader createMetricReader(DataSourceSchema schema) {
        return new MetricJdbcReader(dslContext, getSqlExpressionFormatter());
    }

    @SuppressWarnings("unchecked")
    @Override
    public IMetricCleaner createMetricCleaner(DataSourceSchema schema) {
        return timestamp -> {
            final MetricTable table = new MetricTable(schema);
            dslContext.deleteFrom(table).where(table.getTimestampField().lt(new Timestamp(timestamp))).execute();
        };
    }

    protected ISqlExpressionFormatter getSqlExpressionFormatter() {
        if (dslContext.dialect() == SQLDialect.H2) {
            return MetricJdbcReader.H2SqlExpressionFormatter.INSTANCE;
        } else {
            return MetricJdbcReader.DefaultSqlExpressionFormatter.INSTANCE;
        }
    }

    protected void initialize(DataSourceSchema schema, MetricTable table) {
        CreateTableIndexStep s = dslContext.createTableIfNotExists(table)
                                           .columns(table.fields())
                                           .indexes(table.getIndexes());
        s.execute();
    }
}
