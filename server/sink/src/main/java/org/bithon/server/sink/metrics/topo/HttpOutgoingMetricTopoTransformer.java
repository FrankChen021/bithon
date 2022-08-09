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
import org.bithon.server.sink.common.utils.NetworkUtils;
import org.bithon.server.sink.metrics.EndPointMeasurementBuilder;
import org.bithon.server.sink.metrics.topo.ITopoTransformer;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.Measurement;
import org.bithon.server.storage.meta.EndPointType;
import org.bithon.server.storage.meta.IMetaStorage;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:55 下午
 */
@Slf4j
public class HttpOutgoingMetricTopoTransformer implements ITopoTransformer {

    private final IMetaStorage metaStorage;

    public HttpOutgoingMetricTopoTransformer(IMetaStorage metaStorage) {
        this.metaStorage = metaStorage;
    }

    @Override
    public String getSourceType() {
        return "http-outgoing-metrics";
    }

    @Override
    public Measurement transform(IInputRow message) {
        if (message.getColAsLong("requestCount", 0) <= 0) {
            return null;
        }

        String targetHostPort = message.getColAsString("targetHostPort");
        if (NetworkUtils.isIpAddress(message.getColAsString("targetHost"))) {

            // try to get application info by instance name to see if it's an internal application
            String targetApplicationName = this.metaStorage.getApplicationByInstance(targetHostPort);

            if (targetApplicationName != null) {
                message.updateColumn("targetHostPort", targetApplicationName);
                message.updateColumn("targetType", EndPointType.APPLICATION.getType());

                return EndPointMeasurementBuilder.builder()
                                                 .timestamp(message.getColAsLong("timestamp"))
                                                 .srcEndpointType(EndPointType.APPLICATION)
                                                 .srcEndpoint(message.getColAsString("appName"))
                                                 .dstEndpointType(EndPointType.APPLICATION)
                                                 .dstEndpoint(targetApplicationName)
                                                 .errorCount(message.getColAsLong("countException", 0))
                                                 .interval(message.getColAsLong("interval", 0))
                                                 .callCount(message.getColAsLong("requestCount", 0))
                                                 .responseTime(message.getColAsLong("responseTime", 0))
                                                 .minResponseTime(message.getColAsLong("minResponseTime", 0))
                                                 .maxResponseTime(message.getColAsLong("maxResponseTime", 0))
                                                 .build();
            } else {
                message.updateColumn("targetType", EndPointType.WEB_SERVICE.getType());

                //
                // if the target application has not been in service yet,
                // it of course can't be found in the metadata storage
                //
                // TODO: This record should be fixed when a new instance is inserted into the metadata storage
                return EndPointMeasurementBuilder.builder()
                                                 .timestamp(message.getColAsLong("timestamp"))
                                                 .srcEndpointType(EndPointType.APPLICATION)
                                                 .srcEndpoint(message.getColAsString("appName"))
                                                 .dstEndpointType(EndPointType.WEB_SERVICE)
                                                 .dstEndpoint(targetHostPort)
                                                 .errorCount(message.getColAsLong("countException", 0))
                                                 .interval(message.getColAsLong("interval", 0))
                                                 .callCount(message.getColAsLong("requestCount", 0))
                                                 .responseTime(message.getColAsLong("responseTime", 0))
                                                 .minResponseTime(message.getColAsLong("minResponseTime", 0))
                                                 .maxResponseTime(message.getColAsLong("maxResponseTime", 0))
                                                 .build();
            }
        } else { // if uri.getHost is not IP address
            //TODO: targetHostPort may be an service name if it's a service call such as point to point via service auto discovery
            if (metaStorage.isApplicationExist(targetHostPort)) {
                message.updateColumn("targetType", EndPointType.APPLICATION.getType());

                return EndPointMeasurementBuilder.builder()
                                                 .timestamp(message.getColAsLong("timestamp"))
                                                 .srcEndpointType(EndPointType.APPLICATION)
                                                 .srcEndpoint(message.getColAsString("appName"))
                                                 .dstEndpointType(EndPointType.APPLICATION)
                                                 .dstEndpoint(targetHostPort)
                                                 .errorCount(message.getColAsLong("countException", 0))
                                                 .interval(message.getColAsLong("interval", 0))
                                                 .callCount(message.getColAsLong("requestCount", 0))
                                                 .responseTime(message.getColAsLong("responseTime", 0))
                                                 .minResponseTime(message.getColAsLong("minResponseTime", 0))
                                                 .maxResponseTime(message.getColAsLong("maxResponseTime", 0))
                                                 .build();
            } else {
                message.updateColumn("targetType", EndPointType.WEB_SERVICE.getType());

                return EndPointMeasurementBuilder.builder()
                                                 .timestamp(message.getColAsLong("timestamp"))
                                                 .srcEndpointType(EndPointType.APPLICATION)
                                                 .srcEndpoint(message.getColAsString("appName"))
                                                 .dstEndpointType(EndPointType.WEB_SERVICE)
                                                 .dstEndpoint(targetHostPort)
                                                 .errorCount(message.getColAsLong("countException", 0))
                                                 .interval(message.getColAsLong("interval", 0))
                                                 .callCount(message.getColAsLong("requestCount", 0))
                                                 .responseTime(message.getColAsLong("responseTime", 0))
                                                 .minResponseTime(message.getColAsLong("minResponseTime", 0))
                                                 .maxResponseTime(message.getColAsLong("maxResponseTime", 0))
                                                 .build();
            }
        }
    }
}
