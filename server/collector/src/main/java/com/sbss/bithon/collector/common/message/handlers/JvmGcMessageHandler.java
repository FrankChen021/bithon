package com.sbss.bithon.collector.common.message.handlers;

import com.sbss.bithon.agent.rpc.thrift.service.metric.message.GcEntity;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.JvmMessage;
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
public class JvmGcMessageHandler extends AbstractMetricMessageHandler<JvmMessage> {

    public JvmGcMessageHandler(IMetaStorage metaStorage,
                               IMetricStorage metricStorage,
                               DataSourceSchemaManager dataSourceSchemaManager) throws IOException {
        super("jvm-gc-metrics",
              metaStorage,
              metricStorage,
              dataSourceSchemaManager,
              2,
              10,
              Duration.ofSeconds(60),
              4096);
    }

    @Override
    SizedIterator toIterator(JvmMessage message) {
        Iterator<GcEntity> delegate = message.getGcEntitiesIterator();
        return delegate == null ? null : new SizedIterator() {
            @Override
            public int size() {
                return message.getGcEntitiesSize();
            }

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public GenericMetricObject next() {
                String appName = message.getAppName();
                String instanceName = message.getHostName() + ":" + message.getPort();

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
