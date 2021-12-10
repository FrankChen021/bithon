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

package org.bithon.server.metric.sink;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.common.utils.collection.IteratorableCollection;
import org.bithon.server.meta.storage.IMetaStorage;
import org.bithon.server.metric.DataSourceSchema;
import org.bithon.server.metric.DataSourceSchemaManager;
import org.bithon.server.metric.aggregator.NumberAggregator;
import org.bithon.server.metric.aggregator.spec.IMetricSpec;
import org.bithon.server.metric.input.InputRow;
import org.bithon.server.metric.input.Measurement;
import org.bithon.server.metric.storage.IMetricStorage;
import org.bithon.server.metric.storage.IMetricWriter;

import java.io.IOException;
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
public abstract class AbstractMetricMessageHandler {

    private final DataSourceSchema schema;
    private final DataSourceSchema endpointSchema;
    private final IMetaStorage metaStorage;
    private final IMetricWriter metricStorageWriter;
    private final IMetricWriter endpointMetricStorageWriter;

    public AbstractMetricMessageHandler(String dataSourceName,
                                        IMetaStorage metaStorage,
                                        IMetricStorage metricStorage,
                                        DataSourceSchemaManager dataSourceSchemaManager) throws IOException {

        this.schema = dataSourceSchemaManager.getDataSourceSchema(dataSourceName);
        this.metaStorage = metaStorage;
        this.metricStorageWriter = metricStorage.createMetricWriter(schema);

        this.endpointSchema = dataSourceSchemaManager.getDataSourceSchema("topo-metrics");
        this.endpointSchema.setEnforceDuplicationCheck(false);
        this.endpointMetricStorageWriter = metricStorage.createMetricWriter(endpointSchema);
    }

    public String getType() {
        return this.schema.getName();
    }

    protected boolean beforeProcess(MetricMessage message) throws Exception {
        return true;
    }

    public final void process(IteratorableCollection<MetricMessage> metricMessages) {
        if (!metricMessages.hasNext()) {
            return;
        }

        LocalDataSource endpointDataSource = new LocalDataSource(this.endpointSchema, 30);

        //
        // convert
        //
        List<InputRow> inputRowList = new ArrayList<>(8);
        while (metricMessages.hasNext()) {
            MetricMessage metricMessage = metricMessages.next();

            try {
                if (!beforeProcess(metricMessage)) {
                    continue;
                }

                // extract endpoint
                endpointDataSource.aggregate(extractEndpointLink(metricMessage));

                processMeta(metricMessage);

                inputRowList.add(new InputRow(metricMessage));
            } catch (Exception e) {
                log.error("Failed to process metric object. dataSource=[{}], message=[{}] due to {}",
                          this.schema.getName(),
                          metricMessage,
                          e);
            }
        }

        //
        // save endpoint metrics in batch
        //
        try {
            this.endpointMetricStorageWriter.write(endpointDataSource.toMeasurementList());
        } catch (IOException e) {
            log.error("save metrics", e);
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

    protected Measurement extractEndpointLink(MetricMessage message) {
        return null;
    }

    private void processMeta(MetricMessage metric) {
        String appName = metric.getApplicationName();
        String instanceName = metric.getInstanceName();
        try {
            metaStorage.saveApplicationInstance(appName, metric.getApplicationType(), instanceName);
        } catch (Exception e) {
            log.error("Failed to save app info[appName={}, instance={}] due to: {}",
                      appName,
                      instanceName,
                      e);
        }
    }

    /**
     * key is dimensions
     * value is metrics
     */
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

        /**
         * aggregate the input metrics to a specified time slot metrics
         */
        public void aggregate(Measurement measurement) {
            if (measurement == null) {
                return;
            }

            TimeSlot slotStorage = getSlot(measurement.getTimestamp());

            // get or create metrics
            Map<String, NumberAggregator> metrics = slotStorage.computeIfAbsent(measurement.getDimensions(), dim -> {
                Map<String, NumberAggregator> metricMap = new HashMap<>();
                schema.getMetricsSpec()
                      .forEach((metricSpec) -> {
                          NumberAggregator aggregator = metricSpec.createAggregator();
                          if (aggregator != null) {
                              metricMap.put(metricSpec.getName(), aggregator);
                          }
                      });
                return metricMap;
            });

            Map<String, ? extends Number> inputMetrics = measurement.getMetrics();
            inputMetrics.forEach((metricName, metricValue) -> {
                NumberAggregator aggregator = metrics.computeIfAbsent(metricName,
                                                                      m -> {
                                                                          IMetricSpec spec = schema.getMetricSpecByName(
                                                                              m);
                                                                          if (spec != null) {
                                                                              return spec.createAggregator();
                                                                          }
                                                                          return null;
                                                                      });
                if (aggregator != null) {
                    aggregator.aggregate(measurement.getTimestamp(), metricValue);
                }
            });
        }

        private TimeSlot getSlot(long timestamp) {
            long minutes = timestamp / 60_000;
            int slotIndex = (int) (minutes % timeSlot.length);

            if (timeSlot[slotIndex] == null) {
                timeSlot[slotIndex] = new TimeSlot(minutes * 60_000);
            }
            return timeSlot[slotIndex];
        }

        public List<Measurement> toMeasurementList() {
            List<Measurement> measurementList = new ArrayList<>(8);
            for (TimeSlot slot : timeSlot) {
                if (slot != null) {
                    slot.forEach((dimensions, metrics) -> measurementList.add(new Measurement(slot.getTimestamp(),
                                                                                              dimensions,
                                                                                              metrics)));
                }
            }
            return measurementList;
        }
    }
}
