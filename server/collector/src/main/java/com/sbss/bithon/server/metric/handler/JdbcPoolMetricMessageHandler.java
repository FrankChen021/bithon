package com.sbss.bithon.server.metric.handler;

import com.sbss.bithon.server.common.utils.MiscUtils;
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
public class JdbcPoolMetricMessageHandler extends AbstractMetricMessageHandler {

    public JdbcPoolMetricMessageHandler(IMetaStorage metaStorage,
                                        IMetricStorage metricStorage,
                                        DataSourceSchemaManager dataSourceSchemaManager) throws IOException {
        super("jdbc-pool-metrics",
              metaStorage,
              metricStorage,
              dataSourceSchemaManager,
              2,
              20,
              Duration.ofSeconds(60),
              4096);
    }

    @Override
    protected boolean beforeProcess(GenericMetricMessage metricObject) {
        //TODO: cache the parse result
        MiscUtils.ConnectionString conn = MiscUtils.parseConnectionString(metricObject.getString("connectionString"));
        metricObject.set("server", conn.getHostAndPort());
        metricObject.set("database", conn.getDatabase());
        return true;
    }
}
