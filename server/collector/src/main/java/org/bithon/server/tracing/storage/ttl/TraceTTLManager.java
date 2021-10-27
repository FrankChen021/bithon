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

package org.bithon.server.tracing.storage.ttl;

import lombok.extern.slf4j.Slf4j;
import org.bithon.server.common.utils.ThreadUtils;
import org.bithon.server.common.utils.datetime.DateTimeUtils;
import org.bithon.server.tracing.storage.ITraceCleaner;
import org.bithon.server.tracing.storage.ITraceStorage;
import org.bithon.server.tracing.storage.TraceConfig;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/10/17 10:47
 */
@Slf4j
@Service
public class TraceTTLManager implements SmartLifecycle {

    private final ITraceStorage traceStorage;
    private final TraceConfig traceConfig;
    private ScheduledThreadPoolExecutor executor;

    public TraceTTLManager(ITraceStorage traceStorage, TraceConfig traceConfig) {
        this.traceStorage = traceStorage;
        this.traceConfig = traceConfig;
    }

    @Override
    public void start() {
        log.info("Starting Trace ttl manager and schedule cleanup task for every {}", traceConfig.getCleanPeriod());
        this.executor = new ScheduledThreadPoolExecutor(1, new ThreadUtils.NamedThreadFactory("trace-ttl-manager"));
        this.executor.scheduleAtFixedRate(this::clean,
                                          3,
                                          traceConfig.getCleanPeriod().getMilliseconds(),
                                          TimeUnit.MILLISECONDS);
    }

    private void clean() {
        log.info("Trace clean up starts...");
        {
            long older = System.currentTimeMillis() - traceConfig.getTtl().getMilliseconds();
            log.info("Clean events before {}", DateTimeUtils.toISO8601(older));
            try (ITraceCleaner cleaner = traceStorage.createCleaner()) {
                cleaner.clean(older);
            } catch (Exception e) {
                log.error("Failed to clean events", e);
            }
        }
        log.info("Trace clean up ends...");
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
