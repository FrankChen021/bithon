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

package org.bithon.server.sink.metrics;

import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.metrics.IMetricStorage;

import java.io.IOException;

/**
 * A generic message handler
 *
 * @author frank.chen
 * @date 2022/7/31 11:24
 */
public class MetricMessageHandler extends AbstractMetricMessageHandler {

    public MetricMessageHandler(String dataSourceName,
                                IMetaStorage metaStorage,
                                IMetricStorage metricStorage,
                                DataSourceSchemaManager dataSourceSchemaManager) throws IOException {
        super(dataSourceName,
              metaStorage,
              metricStorage,
              dataSourceSchemaManager);
    }
}
