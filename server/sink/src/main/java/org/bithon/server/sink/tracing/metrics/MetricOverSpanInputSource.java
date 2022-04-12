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

package org.bithon.server.sink.tracing.metrics;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.component.commons.collection.IteratorableCollection;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.server.sink.metrics.IMessageSink;
import org.bithon.server.sink.metrics.MetricMessage;
import org.bithon.server.sink.metrics.SchemaMetricMessage;
import org.bithon.server.sink.tracing.ITraceMessageSink;
import org.bithon.server.sink.tracing.TraceMessageProcessChain;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.TransformSpec;
import org.bithon.server.storage.datasource.aggregator.spec.IMetricSpec;
import org.bithon.server.storage.datasource.dimension.IDimensionSpec;
import org.bithon.server.storage.datasource.source.IInputSource;
import org.bithon.server.storage.tracing.TraceSpan;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 12/4/22 11:27 AM
 */
@JsonTypeName("span")
public class MetricOverSpanInputSource implements IInputSource {

    private static final AtomicInteger REFERENCE_COUNT = new AtomicInteger();
    private static final ExecutorService EXECUTOR;

    static {
        EXECUTOR = new ThreadPoolExecutor(2,
                                          20,
                                          60L,
                                          TimeUnit.SECONDS,
                                          new SynchronousQueue<>(),
                                          NamedThreadFactory.of("span-metrics"));
    }

    private final TraceMessageProcessChain chain;
    private final IMessageSink<SchemaMetricMessage> metricSink;
    private ITraceMessageSink metricExtractor;

    @JsonCreator
    public MetricOverSpanInputSource(@JacksonInject(useInput = OptBoolean.FALSE) TraceMessageProcessChain chain,
                                     @JacksonInject(useInput = OptBoolean.FALSE) IMessageSink<SchemaMetricMessage> metricSink) {
        this.chain = chain;
        this.metricSink = metricSink;
    }

    @Override
    public void start(DataSourceSchema schema) {
        final TransformSpec transformSpec = schema.getTransformSpec();
        if (transformSpec == null) {
            return;
        }
        REFERENCE_COUNT.incrementAndGet();
        metricExtractor = this.chain.link(new MetricOverSpanExtractor(transformSpec, schema, metricSink));
    }

    @Override
    public void stop() {
        if (metricExtractor == null) {
            return;
        }

        try {
            this.chain.unlink(metricExtractor).close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class MetricOverSpanExtractor implements ITraceMessageSink {
        private final TransformSpec transformSpec;
        private final DataSourceSchema schema;
        private final IMessageSink<SchemaMetricMessage> metricSink;

        public MetricOverSpanExtractor(TransformSpec transformSpec,
                                       DataSourceSchema schema,
                                       IMessageSink<SchemaMetricMessage> metricSink) {
            this.transformSpec = transformSpec;
            this.schema = schema;
            this.metricSink = metricSink;
        }

        @Override
        public void process(String messageType, IteratorableCollection<TraceSpan> messages) {
            try {
                Collection<MetricMessage> metricMessages = EXECUTOR.submit(() -> {
                    // transform the spans to target metrics
                    return Collections.synchronizedCollection(messages.toCollection())
                                      .parallelStream()
                                      .filter(transformSpec::transform)
                                      .map(span -> {
                                          MetricMessage metricMessage = new MetricMessage();
                                          metricMessage.setApplicationName(span.getAppName());
                                          metricMessage.setInstanceName(span.getInstanceName());
                                          metricMessage.setTimestamp(span.getStartTime() / 1000);
                                          for (IDimensionSpec dimSpec : schema.getDimensionsSpec()) {
                                              metricMessage.put(dimSpec.getName(), span.getCol(dimSpec.getName()));
                                          }
                                          for (IMetricSpec metricSpec : schema.getMetricsSpec()) {
                                              metricMessage.put(metricSpec.getName(), span.getCol(metricSpec.getName()));
                                          }
                                          metricMessage.set("count", 1);
                                          return metricMessage;
                                      })
                                      .collect(Collectors.toList());

                    // TODO: aggregate the metrics in this batch together
                }).get();

                if (metricMessages.isEmpty()) {
                    return;
                }
                metricSink.process(schema.getName(), SchemaMetricMessage.builder()
                                                                        .schema(schema)
                                                                        .metrics(IteratorableCollection.of(metricMessages.iterator()))
                                                                        .build());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void close() {
            if (REFERENCE_COUNT.decrementAndGet() == 0) {
                EXECUTOR.shutdownNow();
            }
        }
    }
}
