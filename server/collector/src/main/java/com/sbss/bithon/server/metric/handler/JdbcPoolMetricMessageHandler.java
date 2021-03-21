package com.sbss.bithon.server.metric.handler;

import com.sbss.bithon.component.db.dao.EndPointType;
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

//        metricObject.set("endpoint", EndPointLink.builder()
//                                                 .timestamp(metricObject.getTimestamp())
//                                                 .srcEndPointType(EndPointType.APPLICATION)
//                                                 .srcEndpoint(metricObject.getApplicationName())
//                                                 .dstEndpointType(EndPointType.MYSQL)
//                                                 //TODO: extract host and port
//                                                 .dstEndpoint(metricObject.getString("connectionString"))
//                                                 .interval(metricObject.getLong("interval"))
//                                                 .callCount(metricObject.getLong("requestCount"))
//                                                 .responseTime(metricObject.getLong("responseTime"))
//                                                 .minResponseTime(metricObject.getLong("minResponseTime"))
//                                                 .maxResponseTime(metricObject.getLong("maxResponseTime"))
//                                                 .build());
        return true;
    }

    private enum DriverType {
        MYSQL("com.mysql", "mysql"),
        KYLIN("org.apache.kylin", "kylin"),
        UNKNOWN("unknown", "unknown_jdbc");

        private final String classIdentifier;
        private final String driverType;

        DriverType(String classIdentifier, String driverType) {
            this.classIdentifier = classIdentifier;
            this.driverType = driverType;
        }
    }
}
