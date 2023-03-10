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
public class StorageExpirationScheduler {

    private final ScheduledThreadPoolExecutor executor;
    private final ApplicationContext applicationContext;
    private final Map<String, Long> timestamps = new HashMap<>();

    public StorageExpirationScheduler(ApplicationContext applicationContext) {
        this.executor = new ScheduledThreadPoolExecutor(1, NamedThreadFactory.of("storage-cleaner"));
        this.applicationContext = applicationContext;
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
        final long now = System.currentTimeMillis();

        applicationContext.getBeansOfType(IStorage.class)
                          .values()
                          .stream().filter(storage -> storage instanceof IExpirable)
                          .forEach((storage) -> {
                              IStorageCleaner cleaner = ((IExpirable) storage).getCleaner();
                              TTLConfig ttlConfig = cleaner.getTTLConfig();
                              if (ttlConfig == null || !cleaner.getTTLConfig().isEnabled()) {
                                  // In case the configuration changed
                                  return;
                              }

                              long cleanPeriod = ttlConfig.getCleanPeriod().getMilliseconds();

                              long last = timestamps.computeIfAbsent(storage.getName(), v -> now);
                              if ((now - last) < cleanPeriod) {
                                  return;
                              }

                              long before = now - ttlConfig.getTtl().getMilliseconds();
                              log.info("Clean up [{}] before {}...", storage.getClass().getSimpleName(), DateTime.toISO8601(before));
                              try {
                                  cleaner.expire(new Timestamp(before));
                                  timestamps.put(storage.getName(), now);
                                  log.info("Clean up [{}] ends, next round is about at {}", storage.getName(), DateTime.toYYYYMMDDhhmmss(now + cleanPeriod));
                              } catch (Throwable t) {
                                  log.error(StringUtils.format("Clean up [%s] exception: [%s], next round is about at %s",
                                                               storage.getName(),
                                                               t.getMessage(),
                                                               DateTime.toYYYYMMDDhhmmss(now + cleanPeriod)),
                                            t);
                              }
                          });
    }
}
