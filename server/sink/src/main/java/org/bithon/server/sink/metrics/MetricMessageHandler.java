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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.sink.metrics.topo.ITopoTransformer;
import org.bithon.server.sink.metrics.topo.TopoTransformers;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.TransformSpec;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.metrics.IMetricWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * handler per schema, but can be called in multiple threads such as Kafka consumer threads or brpc collector threads
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/3 11:17 下午
 */
@Slf4j
@Getter
public class MetricMessageHandler {
    private static volatile IMetricWriter topoMetricWriter;

    private final ThreadPoolExecutor executor;
    private final DataSourceSchema schema;
    private final DataSourceSchema endpointSchema;
    private final IMetaStorage metaStorage;
    private final IMetricWriter metricWriter;
    private final TopoTransformers topoTransformers;
    private final TransformSpec transformSpec;

    public MetricMessageHandler(String dataSourceName,
                                TopoTransformers topoTransformers,
                                IMetaStorage metaStorage,
                                IMetricStorage metricStorage,
                                DataSourceSchemaManager dataSourceSchemaManager,
                                TransformSpec transformSpec,
                                MetricSinkConfig metricSinkConfig) throws IOException {

        this.topoTransformers = topoTransformers;

        this.schema = dataSourceSchemaManager.getDataSourceSchema(dataSourceName);
        this.metaStorage = metaStorage;
        this.metricWriter = new MetricBatchWriter(dataSourceName, metricStorage.createMetricWriter(schema), metricSinkConfig);

        this.endpointSchema = dataSourceSchemaManager.getDataSourceSchema("topo-metrics");
        this.endpointSchema.setEnforceDuplicationCheck(false);
        if (topoMetricWriter == null) {
            synchronized (MetricMessageHandler.class) {
                if (topoMetricWriter == null) {
                    topoMetricWriter = new MetricBatchWriter(endpointSchema.getName(),
                                                             metricStorage.createMetricWriter(endpointSchema),
                                                             metricSinkConfig);
                }
            }
        }

        this.transformSpec = transformSpec;

        this.executor = new ThreadPoolExecutor(1,
                                               4,
                                               1,
                                               TimeUnit.MINUTES,
                                               new LinkedBlockingQueue<>(1024),
                                               NamedThreadFactory.of(dataSourceName + "-handler"),
                                               new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    public String getType() {
        return this.schema.getName();
    }

    public void process(List<IInputRow> metricMessages) {
        if (CollectionUtils.isEmpty(metricMessages)) {
            return;
        }
        executor.execute(new MetricSinkRunnable(metricMessages));
    }

    class MetricSinkRunnable implements Runnable {
        private List<IInputRow> metricMessages;

        MetricSinkRunnable(List<IInputRow> metricMessages) {
            this.metricMessages = metricMessages;
        }

        @Override
        public void run() {
            //
            // transform the spans to target metrics
            //
            if (transformSpec != null) {
                metricMessages = metricMessages.stream()
                                               .filter(transformSpec::transform)
                                               .collect(Collectors.toList());
            }
            if (metricMessages.isEmpty()) {
                return;
            }

            ITopoTransformer topoTransformer = topoTransformers.getTopoTransformer(getType());
            MetricsAggregator endpointDataSource = new MetricsAggregator(endpointSchema, 60);

            //
            // convert
            //
            List<IInputRow> inputRowList = new ArrayList<>(8);
            for (IInputRow metricMessage : metricMessages) {
                try {
                    // extract endpoint
                    if (topoTransformer != null) {
                        endpointDataSource.aggregate(topoTransformer.transform(metricMessage));
                    }

                    processMeta(metricMessage);

                    inputRowList.add(metricMessage);
                } catch (Exception e) {
                    log.error("Failed to process metric object. dataSource=[{}], message=[{}] due to {}",
                              schema.getName(),
                              metricMessage,
                              e);
                }
            }

            //
            // save endpoint metrics in batch
            //
            try {
                topoMetricWriter.write(endpointDataSource.getRows());
            } catch (IOException e) {
                log.error("save metrics", e);
            }

            //
            // save metrics in batch
            //
            try {
                metricWriter.write(inputRowList);
            } catch (IOException e) {
                log.error("Failed to save metrics [dataSource={}] due to: {}",
                          schema.getName(),
                          e);
            }
        }
    }

    private void processMeta(IInputRow metric) {
        Object appType = metric.getCol("appType");
        if (appType == null) {
            log.warn("Saving meta for [{}] ignored due to lack of appType", this.schema.getName());
            return;
        }

        String appName = metric.getColAsString("appName");
        String instanceName = metric.getColAsString("instanceName");
        try {
            metaStorage.saveApplicationInstance(appName,
                                                appType.toString(),
                                                instanceName);
        } catch (Exception e) {
            log.error("Failed to save app info[appName={}, instance={}] due to: {}",
                      appName,
                      instanceName,
                      e);
        }
    }

    public void close() {
        if (executor.isShutdown() || executor.isTerminated() || executor.isTerminating()) {
            return;
        }

        log.info("Shutting down executor [{}]", schema.getName() + "-handler");
        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }

        try {
            this.metricWriter.close();
        } catch (Exception ignored) {
        }
        synchronized (MetricMessageHandler.class) {
            if (topoMetricWriter != null) {
                try {
                    topoMetricWriter.close();
                } catch (Exception ignored) {
                }
                topoMetricWriter = null;
            }
        }
    }
}
