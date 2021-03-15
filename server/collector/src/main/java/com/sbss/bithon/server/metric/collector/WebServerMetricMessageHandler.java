package com.sbss.bithon.server.metric.collector;

import com.sbss.bithon.agent.rpc.thrift.service.MessageHeader;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.WebServerMetricMessage;
import com.sbss.bithon.server.collector.GenericMessage;
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
public class WebServerMetricMessageHandler extends AbstractMetricMessageHandler {

    public WebServerMetricMessageHandler(IMetaStorage metaStorage,
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
    void toMetricObject(GenericMessage message) {

    }
}
