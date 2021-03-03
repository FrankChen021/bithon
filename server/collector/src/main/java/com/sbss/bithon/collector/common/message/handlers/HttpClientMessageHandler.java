package com.sbss.bithon.collector.common.message.handlers;

import com.sbss.bithon.agent.rpc.thrift.service.metric.message.HttpClientMessage;
import com.sbss.bithon.collector.common.service.UriNormalizer;
import com.sbss.bithon.collector.common.utils.ReflectionUtils;
import com.sbss.bithon.collector.common.utils.datetime.DateTimeUtils;
import com.sbss.bithon.collector.datasource.DataSourceSchemaManager;
import com.sbss.bithon.collector.datasource.storage.IMetricStorage;
import com.sbss.bithon.collector.meta.IMetaStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:55 下午
 */
@Slf4j
@Service
public class HttpClientMessageHandler extends AbstractMetricMessageHandler<HttpClientMessage> {

    private final UriNormalizer uriNormalizer;

    public HttpClientMessageHandler(UriNormalizer uriNormalizer,
                                    IMetaStorage metaStorage,
                                    IMetricStorage metricStorage,
                                    DataSourceSchemaManager dataSourceSchemaManager) throws IOException {
        super("http-client-metrics",
              metaStorage,
              metricStorage,
              dataSourceSchemaManager,
              2,
              20,
              Duration.ofSeconds(60), 4096);
        this.uriNormalizer = uriNormalizer;
    }

    @Override
    SizedIterator toIterator(HttpClientMessage message) {
        if (message.getHttpClient().getRequestCount() <= 0) {
            return null;
        }

        String appName = message.getAppName() + "-" + message.getEnv();
        String instanceName = message.getHostName() + ":" + message.getPort();

        return new SizedIterator() {
            @Override
            public int size() {
                return 1;
            }

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Map<String, Object> next() {
                Map<String, Object> metrics = ReflectionUtils.getFields(message.getHttpClient());

                UriNormalizer.NormalizedResult result = uriNormalizer.normalize(message.getAppName(), message.getHttpClient().getUri());
                if (result.getUri() == null) {
                    return null;
                }

                metrics.put("appName", appName);
                metrics.put("instanceName", instanceName);
                metrics.put("interval", message.getInterval());
                metrics.put("timestamp", DateTimeUtils.dropMilliseconds(message.getTimestamp()));
                metrics.put("uri", result.getUri());
                return metrics;
            }
        };
    }
}
