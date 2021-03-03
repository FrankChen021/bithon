package com.sbss.bithon.collector.common.message.handlers;

import com.sbss.bithon.agent.rpc.thrift.service.metric.message.RedisEntity;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.RedisMessage;
import com.sbss.bithon.collector.common.utils.ReflectionUtils;
import com.sbss.bithon.collector.common.utils.datetime.DateTimeUtils;
import com.sbss.bithon.collector.datasource.DataSourceSchemaManager;
import com.sbss.bithon.collector.datasource.storage.IMetricStorage;
import com.sbss.bithon.collector.meta.IMetaStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:31 下午
 */
@Slf4j
@Service
public class RedisMessageHandler extends AbstractMetricMessageHandler<RedisMessage> {

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
    SizedIterator toIterator(RedisMessage message) {
        String appName = message.getAppName() + "-" + message.getEnv();
        String instanceName = message.getHostName() + ":" + message.getPort();

        Iterator<RedisEntity> delegate = message.getRedisListIterator();
        return delegate == null ? null : new SizedIterator() {
            @Override
            public int size() {
                return message.getRedisList().size();
            }

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public Map<String, Object> next() {
                Map<String, Object> metrics = new HashMap<>();
                metrics.put("appName", appName);
                metrics.put("instanceName", instanceName);
                metrics.put("interval", message.getInterval());
                metrics.put("timestamp", DateTimeUtils.dropMilliseconds(message.getTimestamp()));

                ReflectionUtils.getFields(delegate.next(), metrics);

                return metrics;
            }
        };
    }
}
