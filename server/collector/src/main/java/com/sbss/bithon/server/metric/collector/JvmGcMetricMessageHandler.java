package com.sbss.bithon.server.metric.collector;

import com.sbss.bithon.agent.rpc.thrift.service.MessageHeader;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.JvmMetricMessage;
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
public class JvmGcMetricMessageHandler extends AbstractMetricMessageHandler<MessageHeader, JvmMetricMessage> {

    public JvmGcMetricMessageHandler(IMetaStorage metaStorage,
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
    GenericMetricObject toMetricObject(MessageHeader header, JvmMetricMessage message) {
        GenericMetricObject metrics = new GenericMetricObject(message.getTimestamp(),
                                                              header.getAppName(),
                                                              header.getHostName(),
                                                              message);
        return metrics;
    }
}
