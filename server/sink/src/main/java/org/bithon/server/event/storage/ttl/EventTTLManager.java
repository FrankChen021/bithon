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

package org.bithon.server.event.storage.ttl;

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.server.common.ttl.TTLConfig;
import org.bithon.server.common.utils.datetime.DateTimeUtils;
import org.bithon.server.event.storage.EventStorageConfig;
import org.bithon.server.event.storage.IEventCleaner;
import org.bithon.server.event.storage.IEventStorage;
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
 * @date 2021/10/16 23:15
 */
@Slf4j
@Service
@ConditionalOnProperty(value = "bithon.storage.event.ttl.enabled", havingValue = "true", matchIfMissing = false)
public class EventTTLManager implements SmartLifecycle {

    private final IEventStorage eventStorage;
    private final TTLConfig ttlConfig;
    private ScheduledThreadPoolExecutor executor;

    public EventTTLManager(IEventStorage eventStorage, EventStorageConfig eventStorageConfig) {
        this.eventStorage = eventStorage;
        this.ttlConfig = eventStorageConfig.getTtl();
    }

    @Override
    public void start() {
        log.info("Starting Event ttl manager and schedule cleanup task for every {}", ttlConfig.getCleanPeriod());
        this.executor = new ScheduledThreadPoolExecutor(1, NamedThreadFactory.of("event-ttl-manager"));
        this.executor.scheduleAtFixedRate(this::clean,
                                          3,
                                          ttlConfig.getCleanPeriod().getMilliseconds(),
                                          TimeUnit.MILLISECONDS);
    }

    private void clean() {
        log.info("Event clean up starts...");
        long start = System.currentTimeMillis();
        long older = start - ttlConfig.getTtl().getMilliseconds();
        log.info("Clean events before {}", DateTimeUtils.toISO8601(older));
        try (IEventCleaner cleaner = eventStorage.createCleaner()) {
            cleaner.clean(older);
        } catch (Exception e) {
            log.error("Failed to clean events", e);
        }
        log.info("Event clean up ends, next round is about at {}",
                 new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(new Date(start + ttlConfig.getCleanPeriod().getMilliseconds())));
    }

    @Override
    public void stop() {
        log.info("Shutting down Event ttl manager...");
        executor.shutdownNow();
    }

    @Override
    public boolean isRunning() {
        return executor != null && !executor.isTerminated();
    }
}
