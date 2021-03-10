package com.sbss.bithon.server.metric.collector;

import com.sbss.bithon.agent.rpc.thrift.service.MessageHeader;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.WebServerMetricMessage;
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
 * @date 2021/1/13 11:06 下午
 */
@Slf4j
@Service
public class WebServerMessageHandler extends AbstractMetricMessageHandler<MessageHeader, WebServerMetricMessage> {

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
    SizedIterator toIterator(MessageHeader header, WebServerMetricMessage body) {
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
                ReflectionUtils.getFields(body, metrics);
                return metrics;
            }
        };
    }
}
