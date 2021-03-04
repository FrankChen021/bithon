package com.sbss.bithon.collector.common.message.handlers;

import com.sbss.bithon.agent.rpc.thrift.service.metric.message.JdbcEntity;
import com.sbss.bithon.agent.rpc.thrift.service.metric.message.JdbcMessage;
import com.sbss.bithon.collector.common.utils.ReflectionUtils;
import com.sbss.bithon.collector.datasource.DataSourceSchemaManager;
import com.sbss.bithon.collector.datasource.storage.IMetricStorage;
import com.sbss.bithon.collector.meta.IMetaStorage;
import com.sbss.bithon.component.db.dao.EndPointType;
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
public class JdbcMessageHandler extends AbstractMetricMessageHandler<JdbcMessage> {

    public JdbcMessageHandler(IMetaStorage metaStorage,
                              IMetricStorage metricStorage,
                              DataSourceSchemaManager dataSourceSchemaManager) throws IOException {
        super("jdbc-metrics",
              metaStorage,
              metricStorage,
              dataSourceSchemaManager,
              2,
              20,
              Duration.ofSeconds(60),
              4096);
    }

    @Override
    SizedIterator toIterator(JdbcMessage message) {
        String appName = message.getAppName() + "-" + message.getEnv();
        String instanceName = message.getHostName() + ":" + message.getPort();

        Iterator<JdbcEntity> delegate = message.getJdbcList().iterator();
        return new SizedIterator() {
            @Override
            public int size() {
                return message.getJdbcList().size();
            }

            @Override
            public void close() {
            }

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public GenericMetricObject next() {
                JdbcEntity jdbcEntity = delegate.next();
                GenericMetricObject metrics = new GenericMetricObject(message.getTimestamp(),
                                                                      appName,
                                                                      instanceName);
                metrics.put("interval", message.getInterval());
                metrics.setTargetEndpoint(EndPointType.MYSQL, jdbcEntity.getUri());

                ReflectionUtils.getFields(jdbcEntity, metrics);

                return metrics;
            }
        };
    }
}
