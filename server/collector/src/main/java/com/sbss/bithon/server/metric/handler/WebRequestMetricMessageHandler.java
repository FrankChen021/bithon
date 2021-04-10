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

package com.sbss.bithon.server.metric.handler;

import com.sbss.bithon.component.db.dao.EndPointType;
import com.sbss.bithon.server.common.service.UriNormalizer;
import com.sbss.bithon.server.meta.storage.IMetaStorage;
import com.sbss.bithon.server.metric.DataSourceSchemaManager;
import com.sbss.bithon.server.metric.input.MetricSet;
import com.sbss.bithon.server.metric.storage.IMetricStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Duration;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:55 下午
 */
@Slf4j
@Service
public class WebRequestMetricMessageHandler extends AbstractMetricMessageHandler {

    private final UriNormalizer uriNormalizer;

    public WebRequestMetricMessageHandler(UriNormalizer uriNormalizer,
                                          IMetaStorage metaStorage,
                                          IMetricStorage metricStorage,
                                          DataSourceSchemaManager dataSourceSchemaManager) throws IOException {
        super("web-request-metrics",
              metaStorage,
              metricStorage,
              dataSourceSchemaManager,
              1,
              10,
              Duration.ofSeconds(60),
              4096);
        this.uriNormalizer = uriNormalizer;
    }

    @Override
    protected boolean beforeProcess(GenericMetricMessage message) {
        if (message.getLong("callCount") <= 0) {
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
    protected MetricSet extractEndpointLink(GenericMetricMessage message) {
        String srcApplication;
        EndPointType srcEndPointType;
        if (StringUtils.isEmpty(message.getString("srcApplication"))) {
            srcApplication = "Bithon-Unknown";
            srcEndPointType = EndPointType.UNKNOWN;
        } else {
            srcApplication = message.getString("srcApplication");
            srcEndPointType = EndPointType.APPLICATION;
        }
        return EndPointMetricSetBuilder.builder()
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
                                       .callCount(message.getLong("callCount"))
                                       .build();
    }
}
