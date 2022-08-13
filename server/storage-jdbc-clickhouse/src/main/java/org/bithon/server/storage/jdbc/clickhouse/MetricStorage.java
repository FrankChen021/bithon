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
import org.bithon.component.commons.time.DateTime;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.common.IStorageCleaner;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.jdbc.metric.ISqlExpressionFormatter;
import org.bithon.server.storage.jdbc.metric.MetricJdbcReader;
import org.bithon.server.storage.jdbc.metric.MetricJdbcStorage;
import org.bithon.server.storage.jdbc.metric.MetricTable;
import org.bithon.server.storage.metrics.IMetricReader;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:37 下午
 */
@JsonTypeName("clickhouse")
public class MetricStorage extends MetricJdbcStorage {

    private final ClickHouseSqlExpressionFormatter formatter;
    private final ClickHouseConfig config;

    @JsonCreator
    public MetricStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseJooqContextHolder dslContextHolder,
                         @JacksonInject(useInput = OptBoolean.FALSE) ClickHouseSqlExpressionFormatter formatter,
                         @JacksonInject(useInput = OptBoolean.FALSE) ClickHouseConfig config) {
        super(dslContextHolder.getDslContext());
        this.formatter = formatter;
        this.config = config;
    }

    @Override
    protected void initialize(DataSourceSchema schema, MetricTable table) {
        new TableCreator(config, this.dslContext).createIfNotExist(table);
    }

    @Override
    protected ISqlExpressionFormatter getSqlExpressionFormatter() {
        return formatter;
    }

    @Override
    public IStorageCleaner createMetricCleaner(DataSourceSchema schema) {
        String table = "bithon_" + schema.getName().replace('-', '_');
        return beforeTimestamp -> new DataCleaner(config, dslContext).clean(table, DateTime.toYYYYMMDD(beforeTimestamp.getTime()));
    }

    static class ClickHouseMetricClauseBuilder extends MetricJdbcReader.MetricFieldsClauseBuilder {
        public ClickHouseMetricClauseBuilder(ISqlExpressionFormatter sqlExpressionFormatter,
                                             String sqlTableName,
                                             String tableAlias,
                                             DataSourceSchema dataSource,
                                             Map<String, Object> variables,
                                             boolean addAlias) {
            super(sqlExpressionFormatter, sqlTableName, tableAlias, dataSource, variables, addAlias);
        }

        @Override
        public MetricJdbcReader.MetricFieldsClauseBuilder clone(ISqlExpressionFormatter sqlExpressionFormatter,
                                                                String sqlTableName,
                                                                String tableAlias,
                                                                DataSourceSchema dataSource,
                                                                Map<String, Object> variables,
                                                                boolean addAlias) {
            return new ClickHouseMetricClauseBuilder(sqlExpressionFormatter, sqlTableName, tableAlias, dataSource, variables, addAlias);
        }

        /**
         * for ClickHouse, use argMax to get the last value
         */
        @Override
        protected String visitLast(String metricName) {
            StringBuilder sb = new StringBuilder();
            sb.append(StringUtils.format("argMax(\"%s\", \"timestamp\")", metricName));
            if (addAlias) {
                sb.append(StringUtils.format(" \"%s\"", metricName));
            }
            return sb.toString();
        }
    }

    @Override
    public IMetricReader createMetricReader(DataSourceSchema schema) {
        return new MetricJdbcReader(dslContext, getSqlExpressionFormatter()) {
            @Override
            protected MetricFieldsClauseBuilder createMetriClauseBuilder(String tableName, DataSourceSchema dataSource, Map<String, Object> variables) {
                return new ClickHouseMetricClauseBuilder(sqlFormatter, tableName, "OUTER", dataSource, variables, true);
            }
        };
    }
}
