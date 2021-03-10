package com.sbss.bithon.server.metric.collector;

import com.sbss.bithon.agent.rpc.thrift.service.MessageHeader;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.JvmMetricMessage;
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
public class JvmMessageHandler extends AbstractMetricMessageHandler<MessageHeader, JvmMetricMessage> {

    public JvmMessageHandler(IMetaStorage metaStorage,
                             IMetricStorage metricStorage,
                             DataSourceSchemaManager dataSourceSchemaManager) throws IOException {
        super("jvm-metrics",
              metaStorage,
              metricStorage,
              dataSourceSchemaManager,
              2,
              10,
              Duration.ofSeconds(60),
              4096);
    }

    @Override
    SizedIterator toIterator(MessageHeader header, JvmMetricMessage body) {
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
                metrics.put("interval", body.getInterval());

                ReflectionUtils.getFields(body.getClassesEntity(), metrics);
                ReflectionUtils.getFields(body.getCpuEntity(), metrics);
                ReflectionUtils.getFields(body.getHeapEntity(), metrics);
                ReflectionUtils.getFields(body.getNonHeapEntity(), metrics);
                ReflectionUtils.getFields(body.getMemoryEntity(), metrics);
                ReflectionUtils.getFields(body.getThreadEntity(), metrics);
                ReflectionUtils.getFields(body.getInstanceTimeEntity(), metrics);
                ReflectionUtils.getFields(body.getMetaspaceEntity(), metrics);

                return metrics;
            }
        };
    }
}
