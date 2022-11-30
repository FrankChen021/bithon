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

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.metrics.IMetricWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/30 11:43
 */
@Slf4j
public class MetricBatchWriter implements IMetricWriter {

    private final IMetricWriter delegation;
    private final ScheduledExecutorService executor;
    private final MetricSinkConfig sinkConfig;
    private final String name;
    private List<IInputRow> metricList;

    public MetricBatchWriter(String dataSourceName, IMetricWriter delegation, MetricSinkConfig sinkConfig) {
        this.name = dataSourceName;
        this.delegation = delegation;
        this.sinkConfig = sinkConfig;
        this.metricList = new ArrayList<>(getBatchSize());
        this.executor = new ScheduledThreadPoolExecutor(1, NamedThreadFactory.of(dataSourceName + "-batch-writer"));
        this.executor.scheduleWithFixedDelay(this::flush, 5, sinkConfig.getBatch() == null ? 1 : sinkConfig.getBatch().getInterval(), TimeUnit.SECONDS);
    }

    @Override
    public void write(List<IInputRow> inputRowList) throws IOException {
        synchronized (this) {
            this.metricList.addAll(inputRowList);
        }
        if (this.metricList.size() > getBatchSize()) {
            flush();
        }
    }

    @Override
    public void close() throws Exception {
        log.info("Shutting down metric batch writer...");

        // shutdown and wait for current scheduler to close
        this.executor.shutdown();
        try {
            if (!this.executor.awaitTermination(20, TimeUnit.SECONDS)) {
                log.warn("Timeout when shutdown trace batch writer");
            }
        } catch (InterruptedException ignored) {
        }

        // flush all data to see if there's any more data
        flush();

        // close underlying writer at last
        this.delegation.close();
    }

    /**
     * Get the size from config so that the 'size' can be dynamically in effect if it's changed in configuration center such as nacos/apollo
     */
    private int getBatchSize() {
        return sinkConfig.getBatch() == null ? 2000 : sinkConfig.getBatch().getSize();
    }

    private void flush() {
        if (this.metricList.isEmpty()) {
            return;
        }

        // Swap the object for flushing
        List<IInputRow> flushMetricList;
        synchronized (this) {
            flushMetricList = this.metricList;

            this.metricList = new ArrayList<>(getBatchSize());
        }

        // Double check
        if (flushMetricList.isEmpty()) {
            return;
        }
        try {
            log.debug("Flushing [{}] metrics into storage [{}]...", flushMetricList.size(), this.name);
            this.delegation.write(flushMetricList);
        } catch (Exception e) {
            log.error("Exception when flushing metrics into storage", e);
        }
    }
}
