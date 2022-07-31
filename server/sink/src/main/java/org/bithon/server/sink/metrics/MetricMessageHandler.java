package org.bithon.server.sink.metrics;

import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.metrics.IMetricStorage;

import java.io.IOException;

/**
 * A generic message handler
 *
 * @author frank.chen
 * @date 2022/7/31 11:24
 */
public class MetricMessageHandler extends AbstractMetricMessageHandler {

    public MetricMessageHandler(String dataSourceName,
                                IMetaStorage metaStorage,
                                IMetricStorage metricStorage,
                                DataSourceSchemaManager dataSourceSchemaManager) throws IOException {
        super(dataSourceName,
              metaStorage,
              metricStorage,
              dataSourceSchemaManager);
    }
}
