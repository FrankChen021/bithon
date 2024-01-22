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
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.commons.time.Period;
import org.bithon.server.sink.common.input.IInputSource;
import org.bithon.server.sink.metrics.MetricMessage;
import org.bithon.server.sink.metrics.MetricMessageHandler;
import org.bithon.server.sink.metrics.MetricSinkConfig;
import org.bithon.server.sink.metrics.MetricsAggregator;
import org.bithon.server.sink.tracing.ITraceMessageSink;
import org.bithon.server.sink.tracing.LocalTraceSink;
import org.bithon.server.sink.tracing.TraceSinkConfig;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.TransformSpec;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.context.ApplicationContext;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 12/4/22 11:27 AM
 */
@Slf4j
@JsonTypeName("span")
public class MetricOverSpanInputSource implements IInputSource {

    @JsonIgnore
    private final LocalTraceSink chain;

    @Getter
    private final TransformSpec transformSpec;

    @JsonIgnore
    private final IMetricStorage metricStorage;

    @JsonIgnore
    private final ApplicationContext applicationContext;

    @JsonIgnore
    private MetricOverSpanExtractor metricExtractor;

    @JsonCreator
    public MetricOverSpanInputSource(@JsonProperty("transformSpec") @NotNull TransformSpec transformSpec,
                                     @JacksonInject(useInput = OptBoolean.FALSE) IMetricStorage metricStorage,
                                     @JacksonInject(useInput = OptBoolean.FALSE) ApplicationContext applicationContext) {
        Preconditions.checkArgumentNotNull("transformSpec", transformSpec);

        this.transformSpec = transformSpec;
        this.metricStorage = metricStorage;
        this.applicationContext = applicationContext;

        boolean isEnabled = applicationContext.getBean(TraceSinkConfig.class).isEnabled();
        if (isEnabled) {
            this.chain = applicationContext.getBean(LocalTraceSink.class);
        } else {
            this.chain = null;
        }
    }

    @Override
    public void start(DataSourceSchema schema) {
        if (this.chain == null) {
            log.warn("The trace sink is not enabled in this module. The input source of [{}] has no effect.", schema.getName());
            return;
        }

        try {
            this.metricStorage.createMetricWriter(schema).close();
            log.info("Success to initialize metric storage for [{}].", schema.getName());
        } catch (Exception e) {
            log.info("Failed to initialize metric storage for [{}]: {}", schema.getName(), e.getMessage());
        }
        log.info("Adding metric-extractor for [{}({})] to tracing logs processors...", schema.getName(), schema.getSignature());

        MetricOverSpanExtractor extractor = null;
        try {
            extractor = new MetricOverSpanExtractor(transformSpec, schema, metricStorage, applicationContext);
            metricExtractor = this.chain.link(extractor);
        } catch (Exception e) {
            if (extractor != null) {
                this.chain.unlink(extractor);
                extractor.close();
            }
            throw new RuntimeException(e);
        }
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
        private final MetricMessageHandler metricHandler;

        public MetricOverSpanExtractor(TransformSpec transformSpec,
                                       DataSourceSchema schema,
                                       IMetricStorage metricStorage,
                                       ApplicationContext applicationContext) throws IOException {
            this.transformSpec = transformSpec;
            this.schema = schema;
            this.metricHandler = new MetricMessageHandler(schema.getName(),
                                                          applicationContext.getBean(IMetaStorage.class),
                                                          metricStorage,
                                                          applicationContext.getBean(DataSourceSchemaManager.class),
                                                          null,
                                                          applicationContext.getBean(MetricSinkConfig.class));
        }

        @Override
        public void process(String messageType, List<TraceSpan> spans) {
            if (CollectionUtils.isEmpty(spans)) {
                return;
            }

            //
            // transform the spans to target metrics
            //
            List<IInputRow> metricRows = spans.stream()
                                              .filter(transformSpec::transform)
                                              .map(this::spanToMetrics)
                                              .collect(Collectors.toList());
            if (metricRows.isEmpty()) {
                return;
            }

            //
            // aggregate the metrics together if required
            //
            Period granularity = transformSpec.getGranularity();
            if (granularity != null && granularity.getMilliseconds() > 0) {
                MetricsAggregator aggregator = new MetricsAggregator(schema, granularity);
                metricRows.forEach(aggregator::aggregate);
                metricRows = aggregator.getRows();
            }

            //
            // sink the metrics
            //
            metricHandler.process(metricRows);
        }

        /**
         * will be closed when this processor is unlinked from a processor list
         */
        @Override
        public void close() {
            metricHandler.close();
        }

        private IInputRow spanToMetrics(TraceSpan span) {
            // must be the first.
            // since 'count' is a special name that can be referenced by metricSpec in schema
            span.updateColumn("count", 1);

            MetricMessage metricMessage = new MetricMessage();
            metricMessage.setApplicationName(span.getAppName());
            metricMessage.setInstanceName(span.getInstanceName());
            metricMessage.setTimestamp(span.getStartTime() / 1000);

            for (IColumn dimSpec : schema.getDimensionsSpec()) {
                metricMessage.put(dimSpec.getName(), span.getCol(dimSpec.getName()));
            }
            for (IColumn metricSpec : schema.getMetricsSpec()) {
                String name = metricSpec.getName();
                metricMessage.put(name, span.getCol(name));
            }

            return metricMessage;
        }
    }
}
