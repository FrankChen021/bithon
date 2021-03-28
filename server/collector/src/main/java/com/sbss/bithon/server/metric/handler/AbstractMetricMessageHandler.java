package com.sbss.bithon.server.metric.handler;

import com.sbss.bithon.server.common.handler.AbstractThreadPoolMessageHandler;
import com.sbss.bithon.server.meta.EndPointLink;
import com.sbss.bithon.server.meta.MetadataType;
import com.sbss.bithon.server.meta.storage.IMetaStorage;
import com.sbss.bithon.server.metric.DataSourceSchema;
import com.sbss.bithon.server.metric.DataSourceSchemaManager;
import com.sbss.bithon.server.metric.input.InputRow;
import com.sbss.bithon.server.metric.storage.IMetricStorage;
import com.sbss.bithon.server.metric.storage.IMetricWriter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/3 11:17 下午
 */
@Slf4j
@Getter
public abstract class AbstractMetricMessageHandler extends AbstractThreadPoolMessageHandler<GenericMetricMessage> {

    private final DataSourceSchema schema;
    private final IMetaStorage metaStorage;
    private final IMetricWriter metricStorageWriter;
    private final IMetricWriter topoMetricStorageWriter;

    public AbstractMetricMessageHandler(String dataSourceName,
                                        IMetaStorage metaStorage,
                                        IMetricStorage metricStorage,
                                        DataSourceSchemaManager dataSourceSchemaManager,
                                        int corePoolSize,
                                        int maxPoolSize,
                                        Duration keepAliveTime,
                                        int queueSize) throws IOException {
        super(dataSourceName, corePoolSize, maxPoolSize, keepAliveTime, queueSize);

        this.schema = dataSourceSchemaManager.getDataSourceSchema(dataSourceName);
        this.metaStorage = metaStorage;
        this.metricStorageWriter = metricStorage.createMetricWriter(schema);

        DataSourceSchema topoSchema = dataSourceSchemaManager.getDataSourceSchema("topo-metrics");
        this.topoMetricStorageWriter = metricStorage.createMetricWriter(topoSchema);
    }

    @Override
    public String getType() {
        return this.schema.getName();
    }

    protected boolean beforeProcess(GenericMetricMessage message) throws Exception {
        return true;
    }

    @Override
    final protected void onMessage(GenericMetricMessage metric) {
        try {
            if (beforeProcess(metric)) {
                process(metric);
            }
        } catch (Exception e) {
            log.error("Failed to process metric object. dataSource=[{}], message=[{}] due to {}",
                      this.schema.getName(),
                      metric,
                      e);
        }
    }

    private void process(GenericMetricMessage metric) {
        if (metric == null) {
            return;
        }

        //
        // save application
        //
        String appName = metric.getApplicationName();
        String instanceName = metric.getInstanceName();
        try {
            long appId = metaStorage.getOrCreateMetadataId(appName, MetadataType.APPLICATION, 0L);
            metaStorage.getOrCreateMetadataId(instanceName, MetadataType.APP_INSTANCE, appId);
        } catch (Exception e) {
            log.error("Failed to save app info[appName={}, instance={}] due to: {}",
                      appName,
                      instanceName,
                      e);
        }

        //
        // save dimensions in meta data storage
        //
        /*
        for (IDimensionSpec dimensionSpec : this.schema.getDimensionsSpec()) {
            Object dimensionValue = metricObject.get(dimensionSpec.getName());
            if (dimensionValue == null) {
                continue;
            }
            try {
                this.metaStorage.createMetricDimension(this.schema.getName(),
                                                       dimensionSpec.getName(),
                                                       dimensionValue.toString(),
                                                       metricObject.getTimestamp());
            } catch (Exception e) {
                log.error("Failed to save metrics dimension[dataSource={}, name={}, value={}] due to: {}",
                          this.schema.getName(),
                          dimensionSpec.getName(),
                          dimensionValue,
                          e);
            }
        }
        */

        //
        // save topo
        //
        EndPointLink link = metric.getAs("endpoint");
        if (link != null) {
            try {
                this.topoMetricStorageWriter.write(new InputRow(link));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //
        // save metrics
        //
        try {
            this.metricStorageWriter.write(new InputRow(metric));
        } catch (IOException e) {
            log.error("Failed to save metrics [dataSource={}] due to: {}",
                      this.schema.getName(),
                      e);
        }
    }
}
