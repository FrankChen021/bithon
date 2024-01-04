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

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.time.DateTime;
import org.bithon.component.commons.utils.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/11 17:15
 */
@Slf4j
@Service
public class ExpirationScheduler {

    private final ScheduledThreadPoolExecutor executor;
    private final ApplicationContext applicationContext;

    /**
     * A map that keeps the last timestamp of successful scheduled expiration execution
     */
    private final Map<String, Long> timestamps = new HashMap<>();

    public ExpirationScheduler(ApplicationContext applicationContext) {
        this.executor = new ScheduledThreadPoolExecutor(1, NamedThreadFactory.of("storage-cleaner"));
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void start() {
        log.info("Starting storage cleaner...");
        this.executor.scheduleAtFixedRate(this::expireAllStorages,
                                          1,
                                          1,
                                          TimeUnit.MINUTES);
    }

    @PreDestroy
    public void stop() {
        log.info("Shutting down storage cleaner...");
        executor.shutdownNow();
    }

    private void expireAllStorages() {
        applicationContext.getBeansOfType(IStorage.class)
                          .values()
                          .stream()
                          .filter(storage -> storage instanceof IExpirable)
                          .forEach(this::expireOneStorage);
    }

    private void expireOneStorage(IStorage storage) {
        final long now = System.currentTimeMillis();

        IExpirationRunnable cleaner = ((IExpirable) storage).getExpirationRunnable();
        ExpirationConfig expirationConfig = cleaner.getExpirationConfig();
        if (expirationConfig == null || !cleaner.getExpirationConfig().isEnabled()) {
            // In case the configuration changed
            return;
        }

        long cleanPeriod = expirationConfig.getCleanPeriod().getMilliseconds();

        long last = timestamps.computeIfAbsent(storage.getName(), key -> now);
        if ((now - last) < cleanPeriod) {
            if (log.isDebugEnabled()) {
                log.debug("Storage [{}] is expected to execute expiration at [{}], not now.",
                          storage.getName(),
                          DateTime.toYYYYMMDDhhmmss(last + cleanPeriod));
            }
            return;
        }

        long before = now - expirationConfig.getTtl().getMilliseconds();
        log.info("Expire [{}] before {}...", storage.getName(), DateTime.toYYYYMMDDhhmmss(before));
        try {
            cleaner.expire(new Timestamp(before));
            log.info("Expire [{}] ends, next round is about at [{}]",
                     storage.getName(),
                     DateTime.toYYYYMMDDhhmmss(now + cleanPeriod));
        } catch (Throwable t) {
            log.error(StringUtils.format("Failed to expire [%s]: [%s].",
                                         storage.getName(),
                                         t.getMessage()),
                      t);
        } finally {
            // Update the timestamp whether the task is successful or not.
            // This prevents retrying the task periodically in case it fails.
            timestamps.put(storage.getName(), now);
        }
    }
}
