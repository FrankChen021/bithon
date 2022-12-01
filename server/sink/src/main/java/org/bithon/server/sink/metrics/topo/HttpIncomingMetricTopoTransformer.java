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
import org.bithon.server.sink.metrics.Measurement;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.meta.EndPointType;
import org.springframework.util.StringUtils;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:55 下午
 */
@Slf4j
public class HttpIncomingMetricTopoTransformer implements ITopoTransformer {

    @Override
    public String getSourceType() {
        return "http-incoming-metrics";
    }

    @Override
    public Measurement transform(IInputRow message) {
        String srcApplication;
        EndPointType srcEndPointType;
        if (StringUtils.isEmpty(message.getColAsString("srcApplication"))) {
            srcApplication = "User";
            srcEndPointType = EndPointType.USER;
        } else {
            srcApplication = message.getColAsString("srcApplication");
            srcEndPointType = EndPointType.APPLICATION;
        }
        return EndPointMeasurementBuilder.builder()
                                         .timestamp(message.getColAsLong("timestamp"))
                                         // dimension
                                         .srcEndpointType(srcEndPointType)
                                         .srcEndpoint(srcApplication)
                                         .dstEndpointType(EndPointType.APPLICATION)
                                         .dstEndpoint(message.getColAsString("appName"))
                                         // metric
                                         .interval(message.getColAsLong("interval"))
                                         .minResponseTime(message.getColAsLong("minResponseTime", 0))
                                         .maxResponseTime(message.getColAsLong("maxResponseTime", 0))
                                         .responseTime(message.getColAsLong("responseTime", 0))
                                         .callCount(message.getColAsLong("totalCount", 0))
                                         .errorCount(message.getColAsLong("errorCount", 0))
                                         .build();
    }
}
