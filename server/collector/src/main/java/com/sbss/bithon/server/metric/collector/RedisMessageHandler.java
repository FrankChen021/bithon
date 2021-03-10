package com.sbss.bithon.server.metric.collector;

import com.sbss.bithon.agent.rpc.thrift.service.MessageHeader;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.RedisMetricMessage;
import com.sbss.bithon.component.db.dao.EndPointType;
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
public class RedisMessageHandler extends AbstractMetricMessageHandler<MessageHeader, RedisMetricMessage> {

    public RedisMessageHandler(IMetaStorage metaStorage,
                               IMetricStorage metricStorage,
                               DataSourceSchemaManager dataSourceSchemaManager
    ) throws IOException {
        super("redis-metrics",
              metaStorage,
              metricStorage,
              dataSourceSchemaManager,
              2,
              20,
              Duration.ofSeconds(60),
              4096);
    }

    @Override
    SizedIterator toIterator(MessageHeader header, RedisMetricMessage body) {
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
                GenericMetricObject metrics = new GenericMetricObject(body.getTimestamp(),
                                                                      header.getAppName(),
                                                                      header.getHostName());
                metrics.setEndpointLink(EndPointType.APPLICATION,
                                        header.getAppName(),
                                        EndPointType.REDIS,
                                        body.getUri());

                //TODO: timestamp is overrided
                ReflectionUtils.getFields(body, metrics);

                return metrics;
            }
        };
    }
}
