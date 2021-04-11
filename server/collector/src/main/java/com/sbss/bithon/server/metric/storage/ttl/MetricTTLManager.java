/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.server.metric.storage.ttl;

import com.sbss.bithon.server.common.utils.ThreadUtils;
import com.sbss.bithon.server.common.utils.datetime.DateTimeUtils;
import com.sbss.bithon.server.metric.DataSourceSchema;
import com.sbss.bithon.server.metric.DataSourceSchemaManager;
import com.sbss.bithon.server.metric.storage.IMetricStorage;
import com.sbss.bithon.server.metric.storage.IMetricWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/11 17:15
 */
@Slf4j
@Service
public class MetricTTLManager implements SmartLifecycle {

    private final DataSourceSchemaManager schemaManager;
    private final IMetricStorage metricStorage;
    private ScheduledThreadPoolExecutor executor;

    public MetricTTLManager(DataSourceSchemaManager schemaManager,
                            IMetricStorage metricStorage) {
        this.schemaManager = schemaManager;
        this.metricStorage = metricStorage;
    }

    @Override
    public void start() {
        log.info("Starting metrics ttl manager...");
        this.executor = new ScheduledThreadPoolExecutor(1, new ThreadUtils.NamedThreadFactory("metrics-ttl-manager"));
        this.executor.scheduleAtFixedRate(this::clean,
                                          3,
                                          1,
                                          TimeUnit.MINUTES);
    }

    private void clean() {
        log.info("Metrics clean up starts...");
        for (DataSourceSchema schema : schemaManager.getDataSources().values()) {
            cleanDataSource(schema);
        }
        log.info("Metrics clean up ends...");
    }

    private void cleanDataSource(DataSourceSchema schema) {
        long older = System.currentTimeMillis() - schema.getTtl().getMilliseconds();

        log.info("Clean [{}] before {}", schema.getName(), DateTimeUtils.toISO8601(older));
        try (IMetricWriter writer = metricStorage.createMetricWriter(schema)) {
            writer.deleteBefore(older);
        } catch (Exception e) {
            log.error("Failed to clean " + schema.getName(), e);
        }
    }

    @Override
    public void stop() {
        log.info("Shutting down metrics ttl manager...");
        executor.shutdownNow();
    }

    @Override
    public boolean isRunning() {
        return executor != null && !executor.isTerminated();
    }
}
