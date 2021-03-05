package com.sbss.bithon.collector.common.message.handlers;

import com.sbss.bithon.agent.rpc.thrift.service.metric.message.WebServerMessage;
import com.sbss.bithon.collector.common.utils.ReflectionUtils;
import com.sbss.bithon.collector.datasource.DataSourceSchemaManager;
import com.sbss.bithon.collector.datasource.storage.IMetricStorage;
import com.sbss.bithon.collector.meta.IMetaStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/13 11:06 下午
 */
@Slf4j
@Service
public class WebServerMessageHandler extends AbstractMetricMessageHandler<WebServerMessage> {

    public WebServerMessageHandler(IMetaStorage metaStorage,
                                   IMetricStorage metricStorage,
                                   DataSourceSchemaManager dataSourceSchemaManager) throws IOException {
        super("web-server-metrics",
              metaStorage,
              metricStorage,
              dataSourceSchemaManager,
              5,
              10,
              Duration.ofMinutes(1),
              1024);
    }

    @Override
    SizedIterator toIterator(WebServerMessage message) {
        String appName = message.getAppName();
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
            public GenericMetricObject next() {
                GenericMetricObject metrics = new GenericMetricObject(message.getTimestamp(),
                                                                      appName,
                                                                      instanceName);
                metrics.put("interval", message.getInterval());

                ReflectionUtils.getFields(message.getServerEntity(), metrics);
                return metrics;
            }
        };
    }
}
