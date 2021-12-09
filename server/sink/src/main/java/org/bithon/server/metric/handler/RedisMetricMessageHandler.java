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

package org.bithon.server.metric.handler;

import lombok.extern.slf4j.Slf4j;
import org.bithon.server.common.utils.EndPointType;
import org.bithon.server.meta.storage.IMetaStorage;
import org.bithon.server.metric.DataSourceSchemaManager;
import org.bithon.server.metric.input.Measurement;
import org.bithon.server.metric.storage.IMetricStorage;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:31 下午
 */
@Slf4j
@Service
public class RedisMetricMessageHandler extends AbstractMetricMessageHandler {

    public RedisMetricMessageHandler(IMetaStorage metaStorage,
                                     IMetricStorage metricStorage,
                                     DataSourceSchemaManager dataSourceSchemaManager) throws IOException {
        super("redis-metrics",
              metaStorage,
              metricStorage,
              dataSourceSchemaManager);
    }

    @Override
    protected Measurement extractEndpointLink(MetricMessage metricObject) {
        return EndPointMeasurementBuilder.builder()
                                         .timestamp(metricObject.getTimestamp())
                                         .srcEndpointType(EndPointType.APPLICATION)
                                         .srcEndpoint(metricObject.getApplicationName())
                                         .dstEndpointType(EndPointType.REDIS)
                                         .dstEndpoint(metricObject.getString("uri"))
                                         // metric
                                         .interval(metricObject.getLong("interval"))
                                         .errorCount(metricObject.getLong("exceptionCount"))
                                         .callCount(metricObject.getLong("totalCount"))
                                         .responseTime(metricObject.getLong("responseTime"))
                                         .minResponseTime(metricObject.getLong("minResponseTime"))
                                         .maxResponseTime(metricObject.getLong("maxResponseTime"))
                                         .build();
    }
}
