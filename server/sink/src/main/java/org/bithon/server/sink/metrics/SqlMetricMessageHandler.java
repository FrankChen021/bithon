/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.server.sink.metrics;

import lombok.extern.slf4j.Slf4j;
import org.bithon.server.sink.common.utils.MiscUtils;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.Measurement;
import org.bithon.server.storage.meta.EndPointType;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.metrics.IMetricStorage;

import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/21 21:37
 */
@Slf4j
public class SqlMetricMessageHandler extends AbstractMetricMessageHandler {
    public SqlMetricMessageHandler(IMetaStorage metaStorage,
                                   IMetricStorage metricStorage,
                                   DataSourceSchemaManager dataSourceSchemaManager) throws IOException {
        super("sql-metrics",
              metaStorage,
              metricStorage,
              dataSourceSchemaManager);
    }

    // TODO: cache the process result of connection string

    /**
     * connection string template:
     * jdbc:h2:mem:007af5e4-ee6e-4af5-a515-f961f0fd02a1;a=b
     * jdbc:mysql://localhost:3306/bithon?useUnicode=true&amp;useSSL=false&amp;autoReconnect=TRUE
     * jdbc:netezza://main:5490/sales;user=admin;password=password;loglevel=2
     */
    @Override
    protected boolean beforeProcess(IInputRow metricObject) {
        long callCount = metricObject.getColAsLong("callCount", 0);
        if (callCount <= 0) {
            return false;
        }

        MiscUtils.ConnectionString conn = MiscUtils.parseConnectionString(metricObject.getColAsString("connectionString"));
        metricObject.updateColumn("server", conn.getHostAndPort());
        metricObject.updateColumn("database", conn.getDatabase());
        metricObject.updateColumn("endpointType", conn.getEndPointType().name());
        return true;
    }

    @Override
    protected Measurement extractEndpointLink(IInputRow metricObject) {
        return EndPointMeasurementBuilder.builder()
                                         .timestamp(metricObject.getColAsLong("timestamp"))
                                         .srcEndpointType(EndPointType.APPLICATION)
                                         .srcEndpoint(metricObject.getColAsString("appName"))
                                         .dstEndpointType(metricObject.getColAsString("endpointType"))
                                         .dstEndpoint(metricObject.getColAsString("server"))
                                         .interval(metricObject.getColAsLong("interval", 0))
                                         .callCount(metricObject.getColAsLong("callCount", 0))
                                         .errorCount(metricObject.getColAsLong("errorCount", 0))
                                         .responseTime(metricObject.getColAsLong("responseTime", 0))
                                         .minResponseTime(metricObject.getColAsLong("minResponseTime", 0))
                                         .maxResponseTime(metricObject.getColAsLong("maxResponseTime", 0))
                                         .build();
    }
}
