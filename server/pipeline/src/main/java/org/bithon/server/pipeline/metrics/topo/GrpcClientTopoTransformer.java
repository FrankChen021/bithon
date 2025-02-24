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

package org.bithon.server.pipeline.metrics.topo;


import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.meta.EndPointType;

/**
 * @author frank.chen021@outlook.com
 * @date 24/2/25 8:45 pm
 */
public class GrpcClientTopoTransformer implements ITopoTransformer {

    @Override
    public String getSourceType() {
        return "grpc-client-metrics";
    }

    @Override
    public Measurement transform(IInputRow message) {
        long totalCount = message.getColAsLong("callCount", 0);

        return EndPointMeasurementBuilder.builder()
                                         .timestamp(message.getColAsLong("timestamp"))
                                         .srcEndpointType(EndPointType.APPLICATION)
                                         .srcEndpoint(message.getColAsString("appName"))
                                         .dstEndpointType(EndPointType.GRPC)
                                         .dstEndpoint(message.getColAsString("server"))
                                         // metric
                                         .interval(message.getColAsLong("interval", 0))
                                         .errorCount(message.getColAsLong("errorCount", 0))
                                         .callCount(totalCount)
                                         .responseTime(message.getColAsLong("responseTime", 0))
                                         .minResponseTime(message.getColAsLong("minResponseTime", 0))
                                         .maxResponseTime(message.getColAsLong("maxResponseTime", 0))
                                         .build();
    }
}
