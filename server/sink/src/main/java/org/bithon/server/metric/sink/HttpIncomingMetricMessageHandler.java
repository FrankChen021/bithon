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

package org.bithon.server.metric.sink;

import lombok.extern.slf4j.Slf4j;
import org.bithon.server.common.service.UriNormalizer;
import org.bithon.server.meta.EndPointType;
import org.bithon.server.meta.storage.IMetaStorage;
import org.bithon.server.metric.DataSourceSchemaManager;
import org.bithon.server.metric.input.Measurement;
import org.bithon.server.metric.storage.IMetricStorage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:55 下午
 */
@Slf4j
@Service
public class HttpIncomingMetricMessageHandler extends AbstractMetricMessageHandler {

    private final UriNormalizer uriNormalizer;

    public HttpIncomingMetricMessageHandler(UriNormalizer uriNormalizer,
                                            IMetaStorage metaStorage,
                                            IMetricStorage metricStorage,
                                            DataSourceSchemaManager dataSourceSchemaManager) throws IOException {
        super("http-incoming-metrics",
              metaStorage,
              metricStorage,
              dataSourceSchemaManager);
        this.uriNormalizer = uriNormalizer;
    }

    @Override
    protected boolean beforeProcess(MetricMessage message) {
        if (message.getLong("totalCount") <= 0) {
            return false;
        }

        UriNormalizer.NormalizedResult result = uriNormalizer.normalize(message.getApplicationName(),
                                                                        message.getString("uri"));
        if (result.getUri() == null) {
            return false;
        }
        message.set("uri", result.getUri());

        return true;
    }

    @Override
    protected Measurement extractEndpointLink(MetricMessage message) {
        String srcApplication;
        EndPointType srcEndPointType;
        if (StringUtils.isEmpty(message.getString("srcApplication"))) {
            srcApplication = "User";
            srcEndPointType = EndPointType.USER;
        } else {
            srcApplication = message.getString("srcApplication");
            srcEndPointType = EndPointType.APPLICATION;
        }
        return EndPointMeasurementBuilder.builder()
                                         .timestamp(message.getTimestamp())
                                         // dimension
                                         .srcEndpointType(srcEndPointType)
                                         .srcEndpoint(srcApplication)
                                         .dstEndpointType(EndPointType.APPLICATION)
                                         .dstEndpoint(message.getApplicationName())
                                         // metric
                                         .interval(message.getLong("interval"))
                                         .minResponseTime(message.getLong("minResponseTime"))
                                         .maxResponseTime(message.getLong("maxResponseTime"))
                                         .responseTime(message.getLong("responseTime"))
                                         .callCount(message.getLong("totalCount"))
                                         .errorCount(message.getLong("errorCount"))
                                         .build();
    }
}
