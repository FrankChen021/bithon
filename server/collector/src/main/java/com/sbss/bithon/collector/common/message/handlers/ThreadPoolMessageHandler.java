package com.sbss.bithon.collector.common.message.handlers;

import com.sbss.bithon.agent.rpc.thrift.service.metric.message.ThreadPoolEntity;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.ThreadPoolMessage;
import com.sbss.bithon.collector.common.utils.ReflectionUtils;
import com.sbss.bithon.collector.datasource.DataSourceSchemaManager;
import com.sbss.bithon.collector.datasource.storage.IMetricStorage;
import com.sbss.bithon.collector.meta.IMetaStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:31 下午
 */
@Slf4j
@Service
public class ThreadPoolMessageHandler extends AbstractMetricMessageHandler<ThreadPoolMessage> {

    public ThreadPoolMessageHandler(IMetaStorage metaStorage,
                                    DataSourceSchemaManager dataSourceSchemaManager,
                                    IMetricStorage metricStorage) throws IOException {
        super("thread-pool-metrics",
              metaStorage,
              metricStorage,
              dataSourceSchemaManager,
              5,
              20,
              Duration.ofSeconds(60),
              4096);
    }

    @Override
    SizedIterator toIterator(ThreadPoolMessage message) {
        String appName = message.getAppName() + "-" + message.getEnv();
        String instanceName = message.getHostName() + ":" + message.getPort();

        Iterator<ThreadPoolEntity> delegate = message.getPoolsIterator();
        return new SizedIterator() {
            @Override
            public int size() {
                return message.getPoolsSize();
            }

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public GenericMetricObject next() {
                GenericMetricObject metrics = new GenericMetricObject(message.getTimestamp(),
                                                                      appName,
                                                                      instanceName);
                metrics.put("interval", message.getInterval());

                ReflectionUtils.getFields(delegate.next(), metrics);
                return metrics;
            }
        };
    }
}
