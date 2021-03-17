package com.sbss.bithon.server.metric.handler;

import com.sbss.bithon.server.meta.storage.IMetaStorage;
import com.sbss.bithon.server.metric.DataSourceSchemaManager;
import com.sbss.bithon.server.metric.storage.IMetricStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 9:31 下午
 */
@Slf4j
@Service
public class ExceptionMetricMessageHandler extends AbstractMetricMessageHandler {

    public ExceptionMetricMessageHandler(IMetaStorage metaStorage,
                                         IMetricStorage metricStorage,
                                         DataSourceSchemaManager dataSourceSchemaManager) throws IOException {
        super("exception-metrics",
              metaStorage,
              metricStorage,
              dataSourceSchemaManager,
              1,
              5,
              Duration.ofSeconds(60),
              4096);
    }

    @Override
    void toMetricObject(GenericMetricMessage message) throws Exception {

    }
}
