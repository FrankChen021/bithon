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

import lombok.extern.slf4j.Slf4j;
import org.bithon.server.common.service.UriNormalizer;
import org.bithon.server.common.utils.NetworkUtils;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.datasource.input.Measurement;
import org.bithon.server.storage.meta.EndPointType;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:55 下午
 */
@Slf4j
public class HttpOutgoingMetricMessageHandler extends AbstractMetricMessageHandler {

    private final UriNormalizer uriNormalizer;

    public HttpOutgoingMetricMessageHandler(UriNormalizer uriNormalizer,
                                            IMetaStorage metaStorage,
                                            IMetricStorage metricStorage,
                                            DataSourceSchemaManager dataSourceSchemaManager) throws IOException {
        super("http-outgoing-metrics",
              metaStorage,
              metricStorage,
              dataSourceSchemaManager);
        this.uriNormalizer = uriNormalizer;
    }

    @Override
    protected boolean beforeProcess(MetricMessage metricObject) throws Exception {
        URI uri = new URI(metricObject.getString("path"));
        UriNormalizer.NormalizedResult result = uriNormalizer.normalize(metricObject.getApplicationName(),
                                                                        NetworkUtils.formatUri(uri));
        if (result.getUri() == null) {
            return false;
        }

        String targetHostPort = toHostPort(uri.getHost(), uri.getPort());
        if (targetHostPort == null) {
            log.warn("TargetHost is blank. {}", metricObject);
            return false;
        }

        metricObject.set("targetHost", uri.getHost());
        metricObject.set("targetHostPort", targetHostPort);
        metricObject.set("path", result.getUri());

        return true;
    }

    @Override
    protected Measurement extractEndpointLink(MetricMessage metricObject) {
        if (metricObject.getLong("requestCount") <= 0) {
            return null;
        }

        String targetHostPort = metricObject.getString("targetHostPort");
        if (NetworkUtils.isIpAddress(metricObject.getString("targetHost"))) {

            // try to get application info by instance name to see if its an internal application
            String targetApplicationName = getMetaStorage().getApplicationByInstance(targetHostPort);

            if (targetApplicationName != null) {
                metricObject.set("targetHostPort", targetApplicationName);
                metricObject.set("targetType", EndPointType.APPLICATION.getType());

                return EndPointMeasurementBuilder.builder()
                                                 .timestamp(metricObject.getTimestamp())
                                                 .srcEndpointType(EndPointType.APPLICATION)
                                                 .srcEndpoint(metricObject.getApplicationName())
                                                 .dstEndpointType(EndPointType.APPLICATION)
                                                 .dstEndpoint(targetApplicationName)
                                                 .errorCount(metricObject.getLong("countException"))
                                                 .interval(metricObject.getLong("interval"))
                                                 .callCount(metricObject.getLong("requestCount"))
                                                 .responseTime(metricObject.getLong("responseTime"))
                                                 .minResponseTime(metricObject.getLong("minResponseTime"))
                                                 .maxResponseTime(metricObject.getLong("maxResponseTime"))
                                                 .build();
            } else {
                metricObject.set("targetType", EndPointType.WEB_SERVICE.getType());

                //
                // if the target application has not been in service yet,
                // it of course can't be found in the metadata storage
                //
                // TODO: This record should be fixed when a new instance is inserted into the metadata storage
                return EndPointMeasurementBuilder.builder()
                                                 .timestamp(metricObject.getTimestamp())
                                                 .srcEndpointType(EndPointType.APPLICATION)
                                                 .srcEndpoint(metricObject.getApplicationName())
                                                 .dstEndpointType(EndPointType.WEB_SERVICE)
                                                 .dstEndpoint(targetHostPort)
                                                 .errorCount(metricObject.getLong("countException"))
                                                 .interval(metricObject.getLong("interval"))
                                                 .callCount(metricObject.getLong("requestCount"))
                                                 .responseTime(metricObject.getLong("responseTime"))
                                                 .minResponseTime(metricObject.getLong(
                                                   "minResponseTime"))
                                                 .maxResponseTime(metricObject.getLong(
                                                   "maxResponseTime"))
                                                 .build();
            }
        } else { // if uri.getHost is not IP address
            //TODO: targetHostPort may be an service name if it's a service call such as point to point via service auto discovery
            if (getMetaStorage().isApplicationExist(targetHostPort)) {
                metricObject.set("targetType", EndPointType.APPLICATION.getType());

                return EndPointMeasurementBuilder.builder()
                                                 .timestamp(metricObject.getTimestamp())
                                                 .srcEndpointType(EndPointType.APPLICATION)
                                                 .srcEndpoint(metricObject.getApplicationName())
                                                 .dstEndpointType(EndPointType.APPLICATION)
                                                 .dstEndpoint(targetHostPort)
                                                 .errorCount(metricObject.getLong("countException"))
                                                 .interval(metricObject.getLong("interval"))
                                                 .callCount(metricObject.getLong("requestCount"))
                                                 .responseTime(metricObject.getLong("responseTime"))
                                                 .minResponseTime(metricObject.getLong("minResponseTime"))
                                                 .maxResponseTime(metricObject.getLong("maxResponseTime"))
                                                 .build();
            } else {
                metricObject.set("targetType", EndPointType.WEB_SERVICE.getType());

                return EndPointMeasurementBuilder.builder()
                                                 .timestamp(metricObject.getTimestamp())
                                                 .srcEndpointType(EndPointType.APPLICATION)
                                                 .srcEndpoint(metricObject.getApplicationName())
                                                 .dstEndpointType(EndPointType.WEB_SERVICE)
                                                 .dstEndpoint(targetHostPort)
                                                 .errorCount(metricObject.getLong("countException"))
                                                 .interval(metricObject.getLong("interval"))
                                                 .callCount(metricObject.getLong("requestCount"))
                                                 .responseTime(metricObject.getLong("responseTime"))
                                                 .minResponseTime(metricObject.getLong("minResponseTime"))
                                                 .maxResponseTime(metricObject.getLong("maxResponseTime"))
                                                 .build();
            }
        }
    }

    private String toHostPort(String targetHost, int targetPort) {
        if (StringUtils.isEmpty(targetHost)) {
            return null;
        }

        if (targetPort < 0) {
            return targetHost;
        } else {
            return targetHost + ":" + targetPort;
        }
    }
}
