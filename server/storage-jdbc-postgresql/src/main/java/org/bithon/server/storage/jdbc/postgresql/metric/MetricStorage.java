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

package org.bithon.server.storage.jdbc.postgresql.metric;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.SchemaManager;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.common.dialect.SqlDialectManager;
import org.bithon.server.storage.jdbc.metric.MetricJdbcStorage;
import org.bithon.server.storage.jdbc.metric.MetricTable;
import org.bithon.server.storage.jdbc.postgresql.TableCreator;
import org.bithon.server.storage.metrics.MetricStorageConfig;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/12/29 23:23
 */
public class MetricStorage extends MetricJdbcStorage {
    @JsonCreator
    public MetricStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcStorageProviderConfiguration providerConfiguration,
                         @JacksonInject(useInput = OptBoolean.FALSE) SchemaManager schemaManager,
                         @JacksonInject(useInput = OptBoolean.FALSE) MetricStorageConfig storageConfig,
                         @JacksonInject(useInput = OptBoolean.FALSE) SqlDialectManager sqlDialectManager) {
        super(providerConfiguration.getDslContext(), schemaManager, storageConfig, sqlDialectManager);
    }

    @Override
    protected void initialize(ISchema dataSource, MetricTable table) {
        if (!this.storageConfig.isCreateTable()) {
            return;
        }

        TableCreator.createTableIfNotExists(this.dslContext, table);
    }
}

