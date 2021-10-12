/*
 *    Copyright 2020 bithon.cn
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

package org.bithon.server.metric.storage.jdbc;

import org.bithon.server.metric.DataSourceSchema;
import org.bithon.server.metric.storage.IMetricReader;
import org.bithon.server.metric.storage.IMetricStorage;
import org.bithon.server.metric.storage.IMetricWriter;
import org.jooq.DSLContext;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:37 下午
 */
public class MetricJdbcStorage implements IMetricStorage {

    private final DSLContext dslContext;

    public MetricJdbcStorage(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public IMetricWriter createMetricWriter(DataSourceSchema schema) {
        return new MetricJdbcWriter(dslContext, schema);
    }

    @Override
    public IMetricReader createMetricReader(DataSourceSchema schema) {
        return new MetricJdbcReader(dslContext);
    }
}
