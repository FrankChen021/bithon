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

    public AbstractMetricMessageHandler(String dataSourceName,
                                        IMetaStorage metaStorage,
                                        IMetricStorage storage,
                                        DataSourceSchemaManager dataSourceSchemaManager,
                                        int corePoolSize,
                                        int maxPoolSize,
                                        Duration keepAliveTime,
                                        int queueSize) throws IOException {
        super(corePoolSize, maxPoolSize, keepAliveTime, queueSize);

        this.schema = dataSourceSchemaManager.getDataSourceSchema(dataSourceName);
        this.metaStorage = metaStorage;
        this.metricStorageWriter = storage.createMetricWriter(schema);
    }

    @Override
    public String getType() {
        return this.schema.getName();
    }

    abstract void toMetricObject(GenericMetricMessage message) throws Exception;

    @Override
    final protected void onMessage(GenericMetricMessage message) {
        try {
            toMetricObject(message);
            processMetricObject(message);
        } catch (Exception e) {
            log.error("Failed to process metric object. dataSource=[{}], message=[{}] due to {}",
                      this.schema.getName(),
                      message,
                      e);
        }
    }

    protected boolean beforeProcessMetricObject(GenericMetricMessage metricObject) throws Exception {
        return true;
    }

    private void processMetricObject(GenericMetricMessage metricObject) throws Exception {
        if (metricObject == null) {
            return;
        }

        if (!beforeProcessMetricObject(metricObject)) {
            return;
        }

        //
        // save application
        //
        String appName = metricObject.getApplicationName();
        String instanceName = metricObject.getInstanceName();
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
        EndPointLink link = metricObject.getAs("endpoint");
        if (link != null) {
            metaStorage.createTopo(link);
        }

        //
        // save metrics
        //
        try {
            this.metricStorageWriter.write(new InputRow(metricObject));
        } catch (IOException e) {
            log.error("Failed to save metrics [dataSource={}] due to: {}",
                      this.schema.getName(),
                      e);
        }
    }
}
