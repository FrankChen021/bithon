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

package org.bithon.server.storage.common;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.time.DateTime;
import org.bithon.component.commons.utils.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/11 17:15
 */
@Slf4j
@Component
public class StorageCleanScheduler {

    private final ScheduledThreadPoolExecutor executor;
    private final List<Cleaner> cleaners = Collections.synchronizedList(new ArrayList<>());

    public StorageCleanScheduler() {
        this.executor = new ScheduledThreadPoolExecutor(1, NamedThreadFactory.of("storage-cleaner"));
    }

    public void addCleaner(String name, TTLConfig ttlConfig, Consumer<Timestamp> cleaner) {
        if (ttlConfig == null) {
            log.info("[{}] has no TTL configuration. Cleaner is ignored", name);
            return;
        }
        cleaners.add(Cleaner.builder()
                            .lastCleanedAt(0)
                            .name(name)
                            .ttlConfig(ttlConfig)
                            .impl(cleaner)
                            .build());
    }

    @PostConstruct
    public void start() {
        log.info("Starting storage cleaner...");
        this.executor.scheduleAtFixedRate(this::clean,
                                          1,
                                          1,
                                          TimeUnit.MINUTES);
    }

    @PreDestroy
    public void stop() {
        log.info("Shutting down storage cleaner...");
        executor.shutdownNow();
    }

    private void clean() {
        long now = System.currentTimeMillis();

        Cleaner[] cleaners = this.cleaners.toArray(new Cleaner[0]);
        for (Cleaner cleaner : cleaners) {
            if (!cleaner.ttlConfig.isEnabled()) {
                // in case the configuration changed
                continue;
            }

            long cleanPeriod = cleaner.ttlConfig.getCleanPeriod().getMilliseconds();

            if ((now - cleaner.lastCleanedAt) < cleanPeriod) {
                continue;
            }

            long expiration = now - cleaner.ttlConfig.getTtl().getMilliseconds();
            log.info("Clean up [{}] before {}...", cleaner.name, DateTime.toISO8601(expiration));
            try {
                cleaner.impl.accept(new Timestamp(expiration));
                cleaner.lastCleanedAt = now;
                log.info("Clean up [{}] ends, next round is about at {}", cleaner.name, DateTime.toYYYYMMDDhhmmss(now + cleanPeriod));
            } catch (Throwable t) {
                log.error(StringUtils.format("Clean up [%s] exception: [%s], next round is about at %s",
                                             cleaner.name,
                                             t.getMessage(),
                                             DateTime.toYYYYMMDDhhmmss(now + cleanPeriod)),
                          t);
            }
        }
    }

    @Builder
    static class Cleaner {
        private String name;
        private Consumer<Timestamp> impl;
        private TTLConfig ttlConfig;

        // runtime property
        private long lastCleanedAt;
    }
}
