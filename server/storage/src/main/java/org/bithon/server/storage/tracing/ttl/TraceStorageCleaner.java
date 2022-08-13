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
import org.bithon.server.storage.common.IStorageCleaner;
import org.bithon.server.storage.common.StorageCleanScheduler;
import org.bithon.server.storage.common.TTLConfig;
import org.bithon.server.storage.tracing.ITraceStorage;
import org.bithon.server.storage.tracing.TraceStorageConfig;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/10/17 10:47
 */
@Slf4j
@Service
@ConditionalOnProperty(value = "bithon.storage.tracing.ttl.enabled", havingValue = "true", matchIfMissing = false)
public class TraceStorageCleaner implements ApplicationContextAware {

    private final ITraceStorage traceStorage;
    private final TTLConfig ttlConfig;

    public TraceStorageCleaner(ITraceStorage traceStorage, TraceStorageConfig traceConfig) {
        this.traceStorage = traceStorage;
        this.ttlConfig = traceConfig.getTtl();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        StorageCleanScheduler scheduler = applicationContext.getBean(StorageCleanScheduler.class);
        scheduler.addCleaner("tracing",
                             ttlConfig,
                             (before) -> {
                                 try (IStorageCleaner cleaner = traceStorage.createCleaner()) {
                                     cleaner.clean(before);
                                 }
                             });
    }
}
