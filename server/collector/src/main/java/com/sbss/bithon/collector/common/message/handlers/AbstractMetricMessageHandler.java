package com.sbss.bithon.collector.common.message.handlers;

import com.sbss.bithon.collector.common.utils.collection.CloseableIterator;
import com.sbss.bithon.collector.datasource.DataSourceSchema;
import com.sbss.bithon.collector.datasource.DataSourceSchemaManager;
import com.sbss.bithon.collector.datasource.dimension.IDimensionSpec;
import com.sbss.bithon.collector.datasource.input.InputRow;
import com.sbss.bithon.collector.datasource.storage.IMetricStorage;
import com.sbss.bithon.collector.datasource.storage.IMetricWriter;
import com.sbss.bithon.collector.meta.EndPointLink;
import com.sbss.bithon.collector.meta.IMetaStorage;
import com.sbss.bithon.collector.meta.MetadataType;
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
public abstract class AbstractMetricMessageHandler<MSG_TYPE> extends AbstractThreadPoolMessageHandler<MSG_TYPE> {

    interface SizedIterator extends CloseableIterator<GenericMetricObject> {
        int size();

        @Override
        default void close() {
        }
    }

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

    abstract SizedIterator toIterator(MSG_TYPE message);

    @Override
    final protected void onMessage(MSG_TYPE message) throws Exception {
        SizedIterator iterator = toIterator(message);
        if (iterator == null || iterator.size() <= 0) {
            return;
        }
        if (iterator.size() == 1) {
            // a fast path to avoid submit this task into thread pool again
            try {
                processMetricObject(iterator.next());
            } catch (Exception e) {
                log.error("Failed to process metric object. dataSource=[{}], message=[{}] due to {}", this.schema.getName(), message, e);
            }
            return;
        }

        while (iterator.hasNext()) {
            GenericMetricObject metricObject = iterator.next();
            this.execute(() -> {
                try {
                    processMetricObject(metricObject);
                } catch (Exception e) {
                    log.error("Failed to process metric object. dataSource=[{}], message=[{}] due to {}", this.schema.getName(), message, e);
                }
            });
        }
    }

    protected boolean beforeProcessMetricObject(GenericMetricObject metricObject) throws Exception {
        return true;
    }

    private void processMetricObject(GenericMetricObject metricObject) throws Exception {
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
            long instanceId = metaStorage.getOrCreateMetadataId(instanceName, MetadataType.INSTANCE, appId);
        } catch (Exception e) {
            log.error("Failed to save app info[appName={}, instance={}] due to: {}",
                      appName,
                      instanceName,
                      e);
        }

        //
        // save dimensions in meta data storage
        //
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

        //
        // save topo
        //
        EndPointLink link = metricObject.getEndPointLink();
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
