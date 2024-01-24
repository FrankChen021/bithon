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

package org.bithon.server.sink.tracing;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.sink.common.pipeline.AbstractPipeline;
import org.bithon.server.sink.tracing.exporter.ITraceExporter;
import org.bithon.server.sink.tracing.receiver.ITraceReceiver;
import org.bithon.server.storage.datasource.input.transformer.ITransformer;
import org.bithon.server.storage.tracing.TraceSpan;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 12/4/22 11:38 AM
 */
@Slf4j
public class TracePipeline extends AbstractPipeline<ITraceReceiver, ITraceExporter> {

    public TracePipeline(TracePipelineConfig pipelineConfig, ObjectMapper objectMapper) {
        super(ITraceReceiver.class,
              ITraceExporter.class,
              pipelineConfig,
              objectMapper);
    }

    @Override
    protected void registerProcessor() {
        ITraceProcessor processor = new PipelineProcessor();
        for (ITraceReceiver receiver : this.receivers) {
            receiver.registerProcessor(processor);
        }
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    class PipelineProcessor implements ITraceProcessor {
        public void process(String messageType, List<TraceSpan> spans) {
            if (CollectionUtils.isEmpty(spans)) {
                return;
            }

            Iterator<TraceSpan> iterator = spans.iterator();
            while (iterator.hasNext()) {
                TraceSpan span = iterator.next();

                for (ITransformer transformer : processors) {
                    if (!transformer.transform(span)) {
                        iterator.remove();
                    }
                }
            }

            if (spans.isEmpty()) {
                return;
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
