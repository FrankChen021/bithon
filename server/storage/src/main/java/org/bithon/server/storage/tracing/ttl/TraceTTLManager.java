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

package org.bithon.server.storage.tracing.ttl;

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.time.DateTime;
import org.bithon.server.storage.TTLConfig;
import org.bithon.server.storage.tracing.ITraceCleaner;
import org.bithon.server.storage.tracing.ITraceStorage;
import org.bithon.server.storage.tracing.TraceStorageConfig;
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
 * @date 2021/10/17 10:47
 */
@Slf4j
@Service
@ConditionalOnProperty(value = "bithon.storage.tracing.ttl.enabled", havingValue = "true", matchIfMissing = false)
public class TraceTTLManager implements SmartLifecycle {

    private final ITraceStorage traceStorage;
    private final TTLConfig ttlConfig;
    private ScheduledThreadPoolExecutor executor;

    public TraceTTLManager(ITraceStorage traceStorage, TraceStorageConfig traceConfig) {
        this.traceStorage = traceStorage;
        this.ttlConfig = traceConfig.getTtl();
    }

    @Override
    public void start() {
        log.info("Starting Trace ttl manager and schedule cleanup task for every {}", ttlConfig.getCleanPeriod());
        this.executor = new ScheduledThreadPoolExecutor(1, NamedThreadFactory.of("trace-ttl-manager"));
        this.executor.scheduleAtFixedRate(this::clean,
                                          3,
                                          ttlConfig.getCleanPeriod().getMilliseconds(),
                                          TimeUnit.MILLISECONDS);
    }

    private void clean() {
        log.info("Trace clean up starts...");
        long start = System.currentTimeMillis();
        long older = start - ttlConfig.getTtl().getMilliseconds();
        log.info("Clean events before {}", DateTime.toISO8601(older));
        try (ITraceCleaner cleaner = traceStorage.createCleaner()) {
            cleaner.clean(older);
        } catch (Exception e) {
            log.error("Failed to clean events", e);
        }
        log.info("Trace clean up ends, next round is about at {}",
                 new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(new Date(start + ttlConfig.getCleanPeriod().getMilliseconds())));
    }

    @Override
    public void stop() {
        log.info("Shutting down Trace ttl manager...");
        executor.shutdownNow();
    }

    @Override
    public boolean isRunning() {
        return executor != null && !executor.isTerminated();
    }
}
