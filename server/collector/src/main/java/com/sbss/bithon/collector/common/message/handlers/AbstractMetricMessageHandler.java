package com.sbss.bithon.collector.common.message.handlers;

import com.sbss.bithon.collector.common.utils.collection.CloseableIterator;
import com.sbss.bithon.collector.datasource.DataSourceSchema;
import com.sbss.bithon.collector.datasource.DataSourceSchemaManager;
import com.sbss.bithon.collector.datasource.dimension.IDimensionSpec;
import com.sbss.bithon.collector.datasource.input.InputRow;
import com.sbss.bithon.collector.datasource.storage.IMetricStorage;
import com.sbss.bithon.collector.datasource.storage.IMetricWriter;
import com.sbss.bithon.collector.meta.IMetaStorage;
import com.sbss.bithon.collector.meta.MetadataType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/3 11:17 下午
 */
@Slf4j
public abstract class AbstractMetricMessageHandler<MSG_TYPE> extends AbstractThreadPoolMessageHandler<MSG_TYPE> {

    interface SizedIterator extends CloseableIterator<Map<String, Object>> {
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
    final protected void onMessage(MSG_TYPE message) throws IOException {
        SizedIterator iterator = toIterator(message);
        if (iterator == null) {
            return;
        }
        if (iterator.size() > 1) {
            while (iterator.hasNext()) {
                this.execute(() -> processMetricObject(iterator.next()));
            }
        } else {
            processMetricObject(iterator.next());
        }
    }

    private void processMetricObject(Map<String, Object> metricObject) {
        if (metricObject == null) {
            return;
        }

        String appName = (String) metricObject.get("appName");
        String instanceName = (String) metricObject.get("instanceName");
        try {
            long appId = metaStorage.getOrCreateMetadataId(appName, MetadataType.APPLICATION, 0L);
            long instanceId = metaStorage.getOrCreateMetadataId(instanceName, MetadataType.INSTANCE, appId);
        } catch (Exception e) {
            log.error("Failed to save app info[appName={}, instance={}] due to: {}",
                      appName,
                      instanceName,
                      e);
        }

        // save dimensions in meta data storage
        for (IDimensionSpec dimensionSpec : this.schema.getDimensionsSpec()) {
            Object dimensionValue = metricObject.get(dimensionSpec.getName());
            if (dimensionValue == null) {
                continue;
            }
            try {
                this.metaStorage.saveMetricDimension(this.schema.getName(),
                                                     dimensionSpec.getName(),
                                                     dimensionValue.toString(),
                                                     (long) metricObject.get("timestamp"));
            } catch (Exception e) {
                log.error("Failed to save metrics dimension[dataSource={}, name={}, value={}] due to: {}",
                          this.schema.getName(),
                          dimensionSpec.getName(),
                          dimensionValue,
                          e);
            }
        }

        // save metrics
        try {
            this.metricStorageWriter.write(new InputRow(metricObject));
        } catch (IOException e) {
            log.error("Failed to save metrics [dataSource={}] due to: {}",
                      this.schema.getName(),
                      e);
        }
    }
}
