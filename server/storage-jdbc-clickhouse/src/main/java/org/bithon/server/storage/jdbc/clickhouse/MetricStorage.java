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
import org.bithon.server.metric.DataSourceSchema;
import org.bithon.server.metric.storage.IMetricCleaner;
import org.bithon.server.storage.jdbc.metric.ISqlExpressionFormatter;
import org.bithon.server.storage.jdbc.metric.MetricJdbcStorage;
import org.bithon.server.storage.jdbc.metric.MetricTable;
import org.jooq.DSLContext;

import static org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageAutoConfiguration.BITHON_CLICKHOUSE_DSL;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:37 下午
 */
@JsonTypeName("clickhouse")
public class MetricStorage extends MetricJdbcStorage {

    private final ClickHouseSqlExpressionFormatter formatter;
    private final ClickHouseConfig config;

    @JsonCreator
    public MetricStorage(@JacksonInject(value = BITHON_CLICKHOUSE_DSL, useInput = OptBoolean.FALSE) DSLContext dslContext,
                         @JacksonInject(useInput = OptBoolean.FALSE) ClickHouseSqlExpressionFormatter formatter,
                         @JacksonInject(useInput = OptBoolean.FALSE) ClickHouseConfig config) {
        super(dslContext);
        this.formatter = formatter;
        this.config = config;
    }

    @Override
    protected void initialize(DataSourceSchema schema, MetricTable table) {
        new TableCreator(config, this.dslContext).createIfNotExist(table, config.getTtlDays());
    }

    @Override
    protected ISqlExpressionFormatter getSqlExpressionFormatter() {
        return formatter;
    }

    @Override
    public IMetricCleaner createMetricCleaner(DataSourceSchema schema) {
        String table = "bithon_" + schema.getName().replace('-', '_');
        return beforeTimestamp -> dslContext.execute(StringUtils.format("ALTER TABLE %s.%s %s DELETE WHERE timestamp < '%s'",
                                                                        config.getDatabase(),
                                                                        config.getLocalTableName(table),
                                                                        config.getClusterExpression(),
                                                                        DateTime.toYYYYMMDDhhmmss(beforeTimestamp)));
    }
}
