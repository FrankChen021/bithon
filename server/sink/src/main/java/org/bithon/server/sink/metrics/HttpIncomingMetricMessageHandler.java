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
import org.bithon.server.sink.common.service.UriNormalizer;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.Measurement;
import org.bithon.server.storage.meta.EndPointType;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:55 下午
 */
@Slf4j
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
    protected boolean beforeProcess(IInputRow message) {
        if (message.getColAsLong("totalCount", 0) <= 0) {
            return false;
        }

        UriNormalizer.NormalizedResult result = uriNormalizer.normalize(message.getColAsString("appName"),
                                                                        message.getColAsString("uri"));
        if (result.getUri() == null) {
            return false;
        }
        message.updateColumn("uri", result.getUri());

        return true;
    }

    @Override
    protected Measurement extractEndpointLink(IInputRow message) {
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
