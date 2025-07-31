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

package org.bithon.server.pipeline.metrics.exporter;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.SupplierUtils;
import org.bithon.server.datasource.DefaultSchema;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.input.IInputRow;
import org.bithon.server.datasource.store.IDataStoreSpec;
import org.bithon.server.pipeline.common.transformer.TransformSpec;
import org.bithon.server.pipeline.metrics.MetricPipelineConfig;
import org.bithon.server.pipeline.metrics.MetricsAggregator;
import org.bithon.server.pipeline.metrics.topo.ITopoTransformer;
import org.bithon.server.pipeline.metrics.topo.TopoTransformers;
import org.bithon.server.storage.datasource.SchemaManager;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.meta.Instance;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.metrics.IMetricWriter;
import org.bithon.server.storage.metrics.MetricDataSourceSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * handler per schema, but can be called in multiple threads such as Kafka consumer threads or brpc collector threads
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/3 11:17 下午
 */
@Slf4j
@Getter
public class MetricMessageHandler implements IMetricMessageHandler {
    private static volatile IMetricWriter topoMetricWriter;

    private final Supplier<ThreadPoolExecutor> executorSupplier;
    private final ISchema schema;
    private final ISchema endpointSchema;
    private final IMetaStorage metaStorage;
    private final IMetricWriter metricWriter;
    private final TopoTransformers topoTransformers;
    private final TransformSpec transformSpec;

    public MetricMessageHandler(String dataSourceName,
                                IMetaStorage metaStorage,
                                IMetricStorage metricStorage,
                                SchemaManager schemaManager,
                                TransformSpec transformSpec,
                                MetricPipelineConfig metricPipelineConfig) throws IOException {

        this.topoTransformers = new TopoTransformers(metaStorage);

        this.schema = schemaManager.getSchema(dataSourceName);
        this.metaStorage = metaStorage;

        //
        // This is not beautiful.
        // When the collector receives the metric message, it creates a schema object without a data store spec the spec rely on the metric storage which is not available for the collector module,
        // after the collector forwards the message to the pipeline, the pipelines needs to access the data store spec to create a metric writer.
        // So we need to set the data store spec here.
        //
        if (this.schema.getDataStoreSpec() == null) {
            IDataStoreSpec dataStoreSpec = new MetricDataSourceSpec(metricStorage);
            ((DefaultSchema) this.schema).setDataStoreSpec(dataStoreSpec);
            dataStoreSpec.setSchema(this.schema);
        }

        this.metricWriter = new MetricBatchWriter(dataSourceName, metricStorage.createMetricWriter(schema), metricPipelineConfig);

        this.endpointSchema = schemaManager.getSchema("topo-metrics");
        if (topoMetricWriter == null) {
            synchronized (MetricMessageHandler.class) {
                if (topoMetricWriter == null) {
                    topoMetricWriter = new MetricBatchWriter(endpointSchema.getName(),
                                                             metricStorage.createMetricWriter(endpointSchema),
                                                             metricPipelineConfig);
                }
            }
        }

        this.transformSpec = transformSpec;

        this.executorSupplier = SupplierUtils.cachedWithLock(() -> new ThreadPoolExecutor(1,
                                                                                          4,
                                                                                          1,
                                                                                          TimeUnit.MINUTES,
                                                                                          new LinkedBlockingQueue<>(1024),
                                                                                          NamedThreadFactory.nonDaemonThreadFactory(dataSourceName + "-handler"),
                                                                                          new ThreadPoolExecutor.DiscardOldestPolicy()));
    }

    public String getType() {
        return this.schema.getName();
    }

    public void process(List<IInputRow> metricMessages) {
        if (CollectionUtils.isEmpty(metricMessages)) {
            return;
        }
        executorSupplier.get().execute(new MetricSinkRunnable(metricMessages));
    }

    /**
     * The class is defined as inner class instead of anonymous class or lambda expression,
     * it's because it will be more clear in the tracing log that the class name of 'Runnable' implementation is displayed.
     */
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
            MetricsAggregator endpointDataSource = new MetricsAggregator((DefaultSchema) endpointSchema, 60);
            ApplicationInstanceWriter instanceWriter = new ApplicationInstanceWriter();

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

                    instanceWriter.add(metricMessage);

                    inputRowList.add(metricMessage);
                } catch (Exception e) {
                    log.error("Failed to process metric object. dataSource=[{}], message=[{}]",
                              schema.getName(),
                              metricMessage,
                              e);
                }
            }

            //
            // save endpoint metrics in batch
            //
            if (topoMetricWriter != null) {
                // If it's null, it has been closed in another thread
                try {
                    topoMetricWriter.write(endpointDataSource.getRows());
                } catch (IOException e) {
                    log.error("save metrics", e);
                }
            }

            instanceWriter.write();

            //
            // save metrics in batch
            //
            try {
                metricWriter.write(inputRowList);
            } catch (IOException e) {
                log.error("Failed to save metrics [dataSource={}]",
                          schema.getName(),
                          e);
            }
        }
    }

    class ApplicationInstanceWriter {
        private final List<Instance> instanceList = new ArrayList<>();

        public void add(IInputRow metric) {
            Instance instance = toApplicationInstance(metric);
            if (instance != null) {
                instanceList.add(instance);
            }
        }

        public void write() {
            if (instanceList.isEmpty()) {
                return;
            }

            try {
                metaStorage.saveApplicationInstance(instanceList);
            } catch (Exception e) {
                log.error("Failed to save app info", e);
            }
        }

        private Instance toApplicationInstance(IInputRow metric) {
            Object appType = metric.getCol("appType");
            if (appType == null) {
                return null;
            }

            String appName = metric.getColAsString("appName");
            String instanceName = metric.getColAsString("instanceName");
            return appName == null || instanceName == null ? null : new Instance(appName, appType.toString(), instanceName);
        }
    }

    public void close() {
        //
        // Since the 'close' might be called in some threads that might be different from the thread that call 'process' above,
        // if we check the executorSupplier is initialized first and then shutdown the executor if it's initialized,
        // there might be edge case that the executorSupplier initializes the object after the 'close'.
        //
        // To avoid this, we always call 'get' to initialize the thread pool even though it will be destroyed immediately.
        //
        ThreadPoolExecutor executor = executorSupplier.get();
        if (executor.isShutdown() || executor.isTerminated() || executor.isTerminating()) {
            return;
        }

        log.info("Shutting down executor [{}]", schema.getName() + "-handler");
        executor.shutdown();
        try {
            //noinspection ResultOfMethodCallIgnored
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
