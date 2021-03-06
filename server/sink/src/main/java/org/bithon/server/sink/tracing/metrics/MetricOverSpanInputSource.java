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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.collection.IteratorableCollection;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.commons.time.Period;
import org.bithon.server.sink.metrics.IMessageSink;
import org.bithon.server.sink.metrics.MetricMessage;
import org.bithon.server.sink.metrics.MetricsAggregator;
import org.bithon.server.sink.metrics.SchemaMetricMessage;
import org.bithon.server.sink.tracing.ITraceMessageSink;
import org.bithon.server.sink.tracing.TraceMessageProcessChain;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.aggregator.spec.IMetricSpec;
import org.bithon.server.storage.datasource.dimension.IDimensionSpec;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.IInputSource;
import org.bithon.server.storage.datasource.input.TransformSpec;
import org.bithon.server.storage.tracing.TraceSpan;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 12/4/22 11:27 AM
 */
@Slf4j
@JsonTypeName("span")
public class MetricOverSpanInputSource implements IInputSource {

    @JsonIgnore
    private final TraceMessageProcessChain chain;

    @JsonIgnore
    private final IMessageSink<SchemaMetricMessage> metricSink;

    @Getter
    private final TransformSpec transformSpec;

    @JsonIgnore
    private MetricOverSpanExtractor metricExtractor;

    @JsonCreator
    public MetricOverSpanInputSource(@JsonProperty("transformSpec") @NotNull TransformSpec transformSpec,
                                     @JacksonInject(useInput = OptBoolean.FALSE) TraceMessageProcessChain chain,
                                     @JacksonInject(useInput = OptBoolean.FALSE) IMessageSink<SchemaMetricMessage> metricSink) {
        Preconditions.checkArgumentNotNull("transformSpec", transformSpec);

        this.chain = chain;
        this.metricSink = metricSink;
        this.transformSpec = transformSpec;
    }

    @Override
    public void start(DataSourceSchema schema) {
        log.info("Adding metric-extractor for [{}({})] to tracing logs processors...", schema.getName(), schema.getSignature());
        metricExtractor = this.chain.link(new MetricOverSpanExtractor(transformSpec, schema, metricSink));
    }

    @Override
    public void stop() {
        if (metricExtractor == null) {
            return;
        }

        log.info("Removing metric-extractor for [{}({})] from tracing logs processors...",
                 metricExtractor.schema.getName(),
                 metricExtractor.schema.getSignature());
        try {
            this.chain.unlink(metricExtractor).close();
        } catch (Exception ignored) {
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
        public void process(String messageType, List<TraceSpan> spans) {
            //
            // transform the spans to target metrics
            //
            Collection<IInputRow> metricRows = spans.stream()
                                                    .filter(transformSpec::transform)
                                                    .map(this::spanToMetrics)
                                                    .collect(Collectors.toList());
            if (metricRows.isEmpty()) {
                return;
            }

            //
            // aggregate the metrics together if required
            //
            Period granularity = transformSpec.getGraunularity();
            if (granularity != null && granularity.getMilliseconds() > 0) {
                MetricsAggregator aggregator = new MetricsAggregator(schema, granularity);
                metricRows.forEach(aggregator::aggregate);
                metricRows = aggregator.getRows();
            }

            //
            // sink the metrics
            //
            metricSink.process(schema.getName(), SchemaMetricMessage.builder()
                                                                    .schema(schema)
                                                                    .metrics(IteratorableCollection.of(metricRows.iterator()))
                                                                    .build());
        }

        @Override
        public void close() {
        }

        private IInputRow spanToMetrics(TraceSpan span) {
            // must be the first.
            // since 'count' is a special name that can be referenced by metricSpec in schema
            span.updateColumn("count", 1);

            MetricMessage metricMessage = new MetricMessage();
            metricMessage.setApplicationName(span.getAppName());
            metricMessage.setInstanceName(span.getInstanceName());
            metricMessage.setTimestamp(span.getStartTime() / 1000);

            for (IDimensionSpec dimSpec : schema.getDimensionsSpec()) {
                metricMessage.put(dimSpec.getName(), span.getCol(dimSpec.getName()));
            }
            for (IMetricSpec metricSpec : schema.getMetricsSpec()) {
                String field = metricSpec.getField() == null ? metricSpec.getName() : metricSpec.getField();
                metricMessage.put(metricSpec.getName(), span.getCol(field));
            }

            return metricMessage;
        }
    }
}
