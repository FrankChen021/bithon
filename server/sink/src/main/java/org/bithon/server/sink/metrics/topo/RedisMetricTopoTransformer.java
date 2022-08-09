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

package org.bithon.server.sink.metrics.topo;

import lombok.extern.slf4j.Slf4j;
import org.bithon.server.sink.metrics.EndPointMeasurementBuilder;
import org.bithon.server.sink.metrics.topo.ITopoTransformer;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.Measurement;
import org.bithon.server.storage.meta.EndPointType;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:31 下午
 */
@Slf4j
public class RedisMetricTopoTransformer implements ITopoTransformer {

    @Override
    public String getSourceType() {
        return "redis-metrics";
    }

    @Override
    public Measurement transform(IInputRow message) {
        return EndPointMeasurementBuilder.builder()
                                         .timestamp(message.getColAsLong("timestamp"))
                                         .srcEndpointType(EndPointType.APPLICATION)
                                         .srcEndpoint(message.getColAsString("appName"))
                                         .dstEndpointType(EndPointType.REDIS)
                                         .dstEndpoint(message.getColAsString("uri"))
                                         // metric
                                         .interval(message.getColAsLong("interval", 0))
                                         .errorCount(message.getColAsLong("exceptionCount", 0))
                                         .callCount(message.getColAsLong("totalCount", 0))
                                         .responseTime(message.getColAsLong("responseTime", 0))
                                         .minResponseTime(message.getColAsLong("minResponseTime", 0))
                                         .maxResponseTime(message.getColAsLong("maxResponseTime", 0))
                                         .build();
    }
}
