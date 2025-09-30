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

package org.bithon.agent.observability.tracing.reporter;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.observability.exporter.Exporter;
import org.bithon.agent.observability.exporter.Exporters;
import org.bithon.agent.observability.exporter.IMessageConverter;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/8/5 20:52
 */
public class DefaultReporter implements ITraceReporter {
    private static final ILogAdaptor log = LoggerFactory.getLogger(DefaultReporter.class);

    private final Exporter traceExporter;
    private final ReporterConfig reporterConfig;

    public DefaultReporter() {
        this.traceExporter = Exporters.getOrCreate(Exporters.EXPORTER_NAME_TRACING);
        this.reporterConfig = ConfigurationManager.getInstance().getConfig(ReporterConfig.class);
    }

    @Override
    public ReporterConfig getReporterConfig() {
        return this.reporterConfig;
    }

    @Override
    public void report(List<ITraceSpan> spans) {
        if (spans.isEmpty()) {
            return;
        }

        List<Object> traceMessages = new ArrayList<>(spans.size());

        IMessageConverter messageConverter = traceExporter.getMessageConverter();
        for (ITraceSpan span : spans) {
            Object message = messageConverter.from(span);
            if (message != null) {
                traceMessages.add(message);
            }
        }
        
        try {
            traceExporter.export(traceMessages);
        } catch (Exception e) {
            log.error("exception when sending trace messages.", e);
        }
    }
}
