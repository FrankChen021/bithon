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

package org.bithon.server.pipeline.tracing;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.pipeline.common.pipeline.AbstractPipeline;
import org.bithon.server.pipeline.common.transform.transformer.ITransformer;
import org.bithon.server.pipeline.common.transform.transformer.TransformResult;
import org.bithon.server.pipeline.metrics.input.MetricInputSourceManager;
import org.bithon.server.pipeline.tracing.exporter.ITraceExporter;
import org.bithon.server.pipeline.tracing.metrics.MetricOverTraceInputSource;
import org.bithon.server.pipeline.tracing.receiver.ITraceReceiver;
import org.bithon.server.storage.tracing.TraceSpan;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 12/4/22 11:38 AM
 */
@Slf4j
public class TracePipeline extends AbstractPipeline<ITraceReceiver, ITraceExporter> {

    private final MetricInputSourceManager metricInputSourceManager;

    public TracePipeline(TracePipelineConfig pipelineConfig,
                         MetricInputSourceManager metricInputSourceManager,
                         ObjectMapper objectMapper) {
        super(ITraceReceiver.class,
              ITraceExporter.class,
              pipelineConfig,
              objectMapper);

        this.metricInputSourceManager = metricInputSourceManager;
    }

    @Override
    protected void registerProcessor() {
        // Load all schemas and input sources
        this.metricInputSourceManager.start(MetricOverTraceInputSource.class);

        ITraceProcessor processor = new PipelineProcessor();
        for (ITraceReceiver receiver : this.receivers) {
            receiver.registerProcessor(processor);
        }
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    public TracePipelineConfig getPipelineConfig() {
        return (TracePipelineConfig) pipelineConfig;
    }

    class PipelineProcessor implements ITraceProcessor {
        public void process(String messageType, List<TraceSpan> spans) {
            if (CollectionUtils.isEmpty(spans)) {
                return;
            }

            if (!processors.isEmpty()) {
                spans = spans.parallelStream()
                             .filter((span) -> {
                                 for (ITransformer processor : processors) {
                                     if (processor.transform(span) == TransformResult.DROP) {
                                         return false;
                                     }
                                 }
                                 return true;
                             })
                             .collect(Collectors.toList());
                if (spans.isEmpty()) {
                    return;
                }
            }

            ITraceExporter[] exporterList = exporters.toArray(new ITraceExporter[0]);
            for (ITraceExporter exporter : exporterList) {
                try {
                    exporter.process(messageType, spans);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        @Override
        public void close() throws Exception {
        }
    }
}
