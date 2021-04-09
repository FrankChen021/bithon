package com.sbss.bithon.server.metric.handler;

import com.sbss.bithon.component.db.dao.EndPointType;
import com.sbss.bithon.server.meta.EndPointLink;
import com.sbss.bithon.server.meta.storage.IMetaStorage;
import com.sbss.bithon.server.metric.DataSourceSchemaManager;
import com.sbss.bithon.server.metric.input.MetricSet;
import com.sbss.bithon.server.metric.storage.IMetricStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;

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
              dataSourceSchemaManager,
              1,
              5,
              Duration.ofSeconds(60),
              4096);
    }

    @Override
    protected MetricSet extractEndpointLink(GenericMetricMessage metricObject) {
        return EndPointMetricSetBuilder.builder()
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
