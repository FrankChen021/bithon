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

import org.bithon.agent.observability.exporter.config.ExporterConfig;
import org.bithon.agent.observability.tracing.context.ITraceSpan;

import java.util.ArrayList;
import java.util.List;

/**
 * This reporter batches spans and reports them to the delegate reporter.
 * NOTE: This reporter is NOT thread-safe.
 *
 * @author frank.chen021@outlook.com
 * @date 2025/8/5 20:49
 */
public class BatchReporter implements ITraceReporter {
    private final ITraceReporter delegate;
    private final List<ITraceSpan> batch;
    private final int batchSize;

    public BatchReporter(ITraceReporter delegate, ExporterConfig exporterConfig) {
        this.delegate = delegate;
        this.batchSize = exporterConfig.getBatchSize();
        this.batch = new ArrayList<>(this.batchSize);
    }

    @Override
    public ExporterConfig getExporterConfig() {
        return delegate.getExporterConfig();
    }

    @Override
    public void report(ITraceSpan span) {
        batch.add(span);

        reportIfNeeded();
    }

    @Override
    public void report(List<ITraceSpan> spans) {
        this.batch.addAll(spans);

        reportIfNeeded();
    }

    @Override
    public void flush() {
        if (!batch.isEmpty()) {
            delegate.report(batch);
            batch.clear();
        }
    }

    private void reportIfNeeded() {
        if (batch.size() >= batchSize) {
            delegate.report(batch);
            batch.clear();
        }
    }
}
