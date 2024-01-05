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

package org.bithon.server.storage.jdbc.mysql;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.common.dialect.SqlDialectManager;
import org.bithon.server.storage.jdbc.metric.MetricJdbcStorage;
import org.bithon.server.storage.jdbc.metric.MetricTable;
import org.bithon.server.storage.metrics.MetricStorageConfig;

/**
 * @author Frank Chen
 * @date 4/1/24 10:10 pm
 */
@JsonTypeName("mysql")
public class MetricStorage extends MetricJdbcStorage {

    @JsonCreator
    public MetricStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcStorageProviderConfiguration provider,
                         @JacksonInject(useInput = OptBoolean.FALSE) DataSourceSchemaManager schemaManager,
                         @JacksonInject(useInput = OptBoolean.FALSE) MetricStorageConfig storageConfig,
                         @JacksonInject(useInput = OptBoolean.FALSE) SqlDialectManager sqlDialectManager) {
        super(provider.getDslContext(), schemaManager, storageConfig, sqlDialectManager);
    }

    @Override
    protected MetricTable toMetricTable(DataSourceSchema schema) {
        // MySQL has a limitation on the index length
        return new MetricTable(schema, 64);
    }
}
