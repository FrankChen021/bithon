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

package org.bithon.server.metric.storage.ttl;

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.time.DateTime;
import org.bithon.server.common.ttl.TTLConfig;
import org.bithon.server.metric.DataSourceSchema;
import org.bithon.server.metric.DataSourceSchemaManager;
import org.bithon.server.metric.storage.IMetricCleaner;
import org.bithon.server.metric.storage.IMetricStorage;
import org.bithon.server.metric.storage.MetricStorageConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/11 17:15
 */
@Slf4j
@Service
@ConditionalOnProperty(value = "bithon.storage.metric.ttl.enabled", havingValue = "true", matchIfMissing = false)
public class MetricTTLManager implements SmartLifecycle {

    private final DataSourceSchemaManager schemaManager;
    private final IMetricStorage metricStorage;
    private ScheduledThreadPoolExecutor executor;
    private final TTLConfig ttlConfig;

    public MetricTTLManager(DataSourceSchemaManager schemaManager,
                            IMetricStorage metricStorage,
                            MetricStorageConfig storageConfig) {
        this.schemaManager = schemaManager;
        this.metricStorage = metricStorage;
        this.ttlConfig = storageConfig.getTtl();
    }

    @Override
    public void start() {
        log.info("Starting metrics ttl manager...");
        this.executor = new ScheduledThreadPoolExecutor(1, NamedThreadFactory.of("metrics-ttl-manager"));
        this.executor.scheduleAtFixedRate(this::clean,
                                          3,
                                          ttlConfig.getCleanPeriod().getMilliseconds(),
                                          TimeUnit.MILLISECONDS);
    }

    private void clean() {
        log.info("Metrics clean up starts...");
        long start = System.currentTimeMillis();
        for (DataSourceSchema schema : schemaManager.getDataSources().values()) {
            cleanDataSource(schema);
        }
        log.info("Metrics clean up ends, next round is about at {}",
                 new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(new Date(start + ttlConfig.getCleanPeriod().getMilliseconds())));
    }

    private void cleanDataSource(DataSourceSchema schema) {
        long older = System.currentTimeMillis() - ttlConfig.getTtl().getMilliseconds();

        log.info("Clean [{}] before {}", schema.getName(), DateTime.toISO8601(older));
        try (IMetricCleaner cleaner = metricStorage.createMetricCleaner(schema)) {
            cleaner.clean(older);
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
