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

package org.bithon.server.storage.jdbc.clickhouse.storage;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.component.commons.time.DateTime;
import org.bithon.server.storage.common.IStorageCleaner;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseJooqContextHolder;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseSqlDialect;
import org.bithon.server.storage.jdbc.metric.ISqlDialect;
import org.bithon.server.storage.jdbc.metric.MetricJdbcStorage;
import org.bithon.server.storage.jdbc.metric.MetricTable;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:37 下午
 */
@JsonTypeName("clickhouse")
public class MetricStorage extends MetricJdbcStorage {

    private final ClickHouseSqlDialect sqlDialect;
    private final ClickHouseConfig config;

    @JsonCreator
    public MetricStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseJooqContextHolder dslContextHolder,
                         @JacksonInject(useInput = OptBoolean.FALSE) ClickHouseSqlDialect sqlDialect,
                         @JacksonInject(useInput = OptBoolean.FALSE) ClickHouseConfig config) {
        super(dslContextHolder.getDslContext());
        this.sqlDialect = sqlDialect;
        this.config = config;
    }

    @Override
    protected void initialize(DataSourceSchema schema, MetricTable table) {
        new TableCreator(config, this.dslContext).createIfNotExist(table);
    }

    @Override
    protected ISqlDialect getSqlDialect() {
        return sqlDialect;
    }

    @Override
    public IStorageCleaner createMetricCleaner(DataSourceSchema schema) {
        String table = "bithon_" + schema.getName().replace('-', '_');
        return beforeTimestamp -> new DataCleaner(config, dslContext).clean(table, DateTime.toYYYYMMDD(beforeTimestamp.getTime()));
    }
}