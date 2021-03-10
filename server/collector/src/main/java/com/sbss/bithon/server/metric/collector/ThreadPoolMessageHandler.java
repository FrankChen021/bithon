package com.sbss.bithon.server.metric.collector;

import com.sbss.bithon.agent.rpc.thrift.service.MessageHeader;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.ThreadPoolMetricMessage;
import com.sbss.bithon.server.common.utils.ReflectionUtils;
import com.sbss.bithon.server.meta.storage.IMetaStorage;
import com.sbss.bithon.server.metric.DataSourceSchemaManager;
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
public class ThreadPoolMessageHandler extends AbstractMetricMessageHandler<MessageHeader, ThreadPoolMetricMessage> {

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
    SizedIterator toIterator(MessageHeader header, ThreadPoolMetricMessage message) {
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
                                                                      header.getAppName(),
                                                                      header.getHostName());
                ReflectionUtils.getFields(message, metrics);
                return metrics;
            }
        };
    }
}
