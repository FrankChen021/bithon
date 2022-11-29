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

package org.bithon.server.storage.metrics.ttl;

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.time.DateTime;
import org.bithon.server.commons.time.Period;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.common.IStorageCleaner;
import org.bithon.server.storage.common.StorageCleanScheduler;
import org.bithon.server.storage.common.TTLConfig;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.metrics.IMetricStorage;
import org.bithon.server.storage.metrics.MetricStorageConfig;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/11 17:15
 */
@Slf4j
@Service
@ConditionalOnExpression(value = "${bithon.storage.metric.enabled: false} and ${bithon.storage.metric.ttl.enabled: false}")
public class MetricStorageCleaner implements ApplicationContextAware {

    private final DataSourceSchemaManager schemaManager;
    private final IMetricStorage metricStorage;
    private final TTLConfig ttlConfig;

    public MetricStorageCleaner(DataSourceSchemaManager schemaManager, IMetricStorage metricStorage, MetricStorageConfig storageConfig) {
        this.schemaManager = schemaManager;
        this.metricStorage = metricStorage;
        this.ttlConfig = storageConfig.getTtl();
    }

    private void cleanDataSource(DataSourceSchema schema, Timestamp before) {
        if (schema.isVirtual()) {
            return;
        }

        Period dataSourceLevelTTL = schema.getTtl();
        if (dataSourceLevelTTL != null && dataSourceLevelTTL.getMilliseconds() != 0) {
            //use datasource ttl
            before = TimeSpan.nowInMinute()
                             .before(dataSourceLevelTTL.getMilliseconds(), TimeUnit.MILLISECONDS)
                             .toTimestamp();
        }

        log.info("\tClean up [{}] before {}", schema.getName(), DateTime.toYYYYMMDDhhmmss(before));
        try (IStorageCleaner cleaner = metricStorage.createMetricCleaner(schema)) {
            cleaner.clean(before);
        } catch (Exception e) {
            log.error("Failed to clean " + schema.getName(), e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        StorageCleanScheduler scheduler = applicationContext.getBean(StorageCleanScheduler.class);
        scheduler.addCleaner("metrics",
                             ttlConfig,
                             (before) -> {
                                 for (DataSourceSchema schema : schemaManager.getDataSources().values()) {
                                     cleanDataSource(schema, before);
                                 }
                             });
    }
}
