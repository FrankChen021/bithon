package com.sbss.bithon.server.metric.handler;

import com.sbss.bithon.component.db.dao.EndPointType;
import com.sbss.bithon.server.common.utils.MiscUtils;
import com.sbss.bithon.server.meta.EndPointLink;
import com.sbss.bithon.server.meta.storage.IMetaStorage;
import com.sbss.bithon.server.metric.DataSourceSchemaManager;
import com.sbss.bithon.server.metric.storage.IMetricStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/21 21:37
 */
@Slf4j
@Service
public class SqlMetricMessageHandler extends AbstractMetricMessageHandler {
    public SqlMetricMessageHandler(IMetaStorage metaStorage,
                                   IMetricStorage metricStorage,
                                   DataSourceSchemaManager dataSourceSchemaManager) throws IOException {
        super("sql-metrics",
              metaStorage,
              metricStorage,
              dataSourceSchemaManager,
              1,
              10,
              Duration.ofSeconds(60),
              1024);
    }

    // TODO: cache the process result of connection string

    /**
     * connection string template:
     * jdbc:h2:mem:007af5e4-ee6e-4af5-a515-f961f0fd02a1;a=b
     * jdbc:mysql://localhost:3306/bithon?useUnicode=true&useSSL=false&autoReconnect=TRUE
     * jdbc:netezza://main:5490/sales;user=admin;password=password;loglevel=2
     */
    @Override
    protected boolean beforeProcess(GenericMetricMessage metricObject) {
        long callCount = metricObject.getLong("callCount");
        if (callCount <= 0) {
            return false;
        }

        MiscUtils.ConnectionString conn = MiscUtils.parseConnectionString(metricObject.getString("connectionString"));
        metricObject.set("server", conn.getHostAndPort());
        metricObject.set("database", conn.getDatabase());
        metricObject.set("endpoint", EndPointLink.builder()
                                                 .timestamp(metricObject.getTimestamp())
                                                 .srcEndpointType(EndPointType.APPLICATION)
                                                 .srcEndpoint(metricObject.getApplicationName())
                                                 .dstEndpointType(conn.getEndPointType())
                                                 .dstEndpoint(conn.getHostAndPort())
                                                 .interval(metricObject.getLong("interval"))
                                                 .callCount(metricObject.getLong("callCount"))
                                                 .errorCount(metricObject.getLong("errorCount"))
                                                 .responseTime(metricObject.getLong("responseTime"))
                                                 .minResponseTime(metricObject.getLong("minResponseTime"))
                                                 .maxResponseTime(metricObject.getLong("maxResponseTime"))
                                                 .build());
        return true;
    }
}
