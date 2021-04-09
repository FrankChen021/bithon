package com.sbss.bithon.server.metric.handler;

import com.sbss.bithon.server.common.handler.AbstractThreadPoolMessageHandler;
import com.sbss.bithon.server.common.utils.collection.SizedIterator;
import com.sbss.bithon.server.meta.EndPointLink;
import com.sbss.bithon.server.meta.MetadataType;
import com.sbss.bithon.server.meta.storage.IMetaStorage;
import com.sbss.bithon.server.metric.DataSourceSchema;
import com.sbss.bithon.server.metric.DataSourceSchemaManager;
import com.sbss.bithon.server.metric.aggregator.NumberAggregator;
import com.sbss.bithon.server.metric.input.InputRow;
import com.sbss.bithon.server.metric.input.MetricSet;
import com.sbss.bithon.server.metric.storage.IMetricStorage;
import com.sbss.bithon.server.metric.storage.IMetricWriter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/3 11:17 下午
 */
@Slf4j
@Getter
public abstract class AbstractMetricMessageHandler
    extends AbstractThreadPoolMessageHandler<SizedIterator<GenericMetricMessage>> {

    private final DataSourceSchema schema;
    private final DataSourceSchema topoSchema;
    private final IMetaStorage metaStorage;
    private final IMetricWriter metricStorageWriter;
    private final IMetricWriter endpointMetricStorageWriter;


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

        this.topoSchema = dataSourceSchemaManager.getDataSourceSchema("topo-metrics");
        this.endpointMetricStorageWriter = metricStorage.createMetricWriter(topoSchema);
    }

    @Override
    public String getType() {
        return this.schema.getName();
    }

    protected boolean beforeProcess(GenericMetricMessage message) throws Exception {
        return true;
    }

    @Override
    final protected void onMessage(SizedIterator<GenericMetricMessage> metricMessages) {
        if (metricMessages.isEmpty()) {
            return;
        }

        LocalDataSource endpointDataSource = new LocalDataSource(this.topoSchema, 30);

        //
        // convert
        //
        List<InputRow> inputRowList = new ArrayList<>(metricMessages.size());
        while (metricMessages.hasNext()) {
            GenericMetricMessage metric = metricMessages.next();

            // extract endpoint
            MetricSet metricSet = extractEndpointLink(metric);
            if (metricSet != null) {
                endpointDataSource.aggregate(metricSet);
            }

            try {
                if (beforeProcess(metric)) {
                    process(metric);
                    inputRowList.add(new InputRow(metric));
                }
            } catch (Exception e) {
                log.error("Failed to process metric object. dataSource=[{}], message=[{}] due to {}",
                          this.schema.getName(),
                          metric,
                          e);
            }
        }

        //
        // save endpoint metrics in batch
        //
        try {
            this.endpointMetricStorageWriter.write(endpointDataSource.toMetricSetList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        //
        // save metrics in batch
        //
        try {
            this.metricStorageWriter.write(inputRowList);
        } catch (IOException e) {
            log.error("Failed to save metrics [dataSource={}] due to: {}",
                      this.schema.getName(),
                      e);
        }
    }

    protected abstract MetricSet extractEndpointLink(GenericMetricMessage message);

    static class TimeSlot extends HashMap<Map<String, String>, Map<String, NumberAggregator>> {
        @Getter
        private final long timestamp;

        TimeSlot(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    static class LocalDataSource {
        private final DataSourceSchema schema;
        private final TimeSlot[] timeSlot;

        public LocalDataSource(DataSourceSchema schema, int minutes) {
            this.schema = schema;
            this.timeSlot = new TimeSlot[minutes];
        }

        public void aggregate(MetricSet endpointLink) {
            TimeSlot slotStorage = getSlot(endpointLink.getTimestamp());

            Map<String, NumberAggregator> metrics = slotStorage.computeIfAbsent(endpointLink.getDimensions(), dim -> {
                Map<String, NumberAggregator> metricMap = new HashMap<>();
                schema.getMetricsSpec().forEach((metricSpec) -> metricMap.put(metricSpec.getName(), metricSpec.createAggregator()));
                return metricMap;
            });

            Map<String, ? extends Number> inputMetrics = endpointLink.getMetrics();
            inputMetrics.forEach((metricName, metricValue) -> {
                NumberAggregator aggregator = metrics.computeIfAbsent(metricName,
                                                                      m -> schema.getMetricSpecByName(m)
                                                                                 .createAggregator());
                aggregator.aggregate(endpointLink.getTimestamp(), metricValue);
            });
        }

        private TimeSlot getSlot(long timestamp) {
            int slotIndex = (int) ((timestamp / 1000 / 60) % 60);
            if (timeSlot[slotIndex] == null) {
                timeSlot[slotIndex] = new TimeSlot(timestamp);
            }
            return timeSlot[slotIndex];
        }

        public List<MetricSet> toMetricSetList() {
            List<MetricSet> metricSetList = new ArrayList<>(8);
            for (TimeSlot slot : timeSlot) {
                if (slot != null) {
                    slot.forEach((dimensions, metrics) -> metricSetList.add(new MetricSet(slot.getTimestamp(), dimensions, metrics)));
                }
            }
            return metricSetList;
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
                this.endpointMetricStorageWriter.write(new InputRow(link));
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
