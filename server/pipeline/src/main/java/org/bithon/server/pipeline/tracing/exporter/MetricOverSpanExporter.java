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

package org.bithon.server.pipeline.tracing.exporter;

import lombok.Getter;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.commons.time.Period;
import org.bithon.server.pipeline.common.transform.TransformSpec;
import org.bithon.server.pipeline.metrics.MetricMessage;
import org.bithon.server.pipeline.metrics.MetricPipelineConfig;
import org.bithon.server.pipeline.metrics.MetricsAggregator;
import org.bithon.server.pipeline.metrics.exporter.MetricMessageHandler;
import org.bithon.server.storage.datasource.DefaultSchema;
import org.bithon.server.storage.datasource.SchemaManager;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.tracing.TraceSpan;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 28/3/24 12:00 pm
 */
public class MetricOverSpanExporter implements ITraceExporter {
    private final TransformSpec transformSpec;

    @Getter
    private final DefaultSchema schema;
    private final MetricMessageHandler metricHandler;

    public MetricOverSpanExporter(TransformSpec transformSpec,
                                  DefaultSchema schema,
                                  IMetricStorage metricStorage,
                                  ApplicationContext applicationContext) throws IOException {
        this.transformSpec = transformSpec;
        this.schema = schema;
        this.metricHandler = new MetricMessageHandler(schema.getName(),
                                                      applicationContext.getBean(IMetaStorage.class),
                                                      metricStorage,
                                                      applicationContext.getBean(SchemaManager.class),
                                                      null,
                                                      applicationContext.getBean(MetricPipelineConfig.class));
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
        // since 'count' is a special name that metricSpec can reference in the schema
        span.updateColumn("count", 1);

        MetricMessage metricMessage = new MetricMessage();
        for (IColumn column : schema.getColumns()) {
            metricMessage.put(column.getName(), span.getCol(column.getName()));
        }
        metricMessage.setApplicationName(span.getAppName());
        metricMessage.setInstanceName(span.getInstanceName());
        metricMessage.setTimestamp(span.getStartTime() / 1000);

        return metricMessage;
    }

    @Override
    public String toString() {
        return "MetricOverSpanInputSource[" + this.schema.getName() + "]";
    }
}
