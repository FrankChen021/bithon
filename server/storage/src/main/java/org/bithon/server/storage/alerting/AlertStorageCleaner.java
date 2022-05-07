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

package org.bithon.server.storage.alerting;

import org.bithon.server.storage.common.IStorageCleaner;
import org.bithon.server.storage.common.StorageCleanScheduler;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author Frank Chen
 * @date 10/4/22 8:55 PM
 */
public class AlertStorageCleaner implements ApplicationContextAware {

    final IEvaluationLogStorage evaluatorLoggerStorage;
    final IAlertRecordStorage recordStorage;
    private final AlertingStorageConfiguration.EvaluationLogConfig logConfig;
    private final AlertingStorageConfiguration.AlertRecordConfig recordConfig;

    public AlertStorageCleaner(IEvaluationLogStorage evaluatorLoggerStorage,
                               AlertingStorageConfiguration.EvaluationLogConfig logConfig,
                               IAlertRecordStorage recordStorage,
                               AlertingStorageConfiguration.AlertRecordConfig recordConfig) {
        this.evaluatorLoggerStorage = evaluatorLoggerStorage;
        this.logConfig = logConfig;
        this.recordStorage = recordStorage;
        this.recordConfig = recordConfig;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        StorageCleanScheduler scheduler = applicationContext.getBean(StorageCleanScheduler.class);

        scheduler.addCleaner("alerting-evaluation-logs",
                             logConfig.getTtl(),
                             (before) -> {
                                 try (IStorageCleaner cleaner = evaluatorLoggerStorage.createCleaner()) {
                                     cleaner.clean(before);
                                 }
                             });
        scheduler.addCleaner("alerting-records",
                             recordConfig.getTtl(),
                             (before) -> {
                                 try (IStorageCleaner cleaner = recordStorage.createCleaner()) {
                                     cleaner.clean(before);
                                 }
                             });
    }
}
