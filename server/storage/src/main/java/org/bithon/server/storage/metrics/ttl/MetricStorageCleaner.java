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
import org.bithon.server.storage.common.IExpirationRunnable;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/11 17:15
 */
@Slf4j
public abstract class MetricStorageCleaner implements IExpirationRunnable {

    protected abstract DataSourceSchemaManager getSchemaManager();

    @Override
    public void expire(Timestamp before) {
        for (DataSourceSchema schema : getSchemaManager().getDataSources().values()) {
            expire(schema, before);
        }
    }

    private void expire(DataSourceSchema schema, Timestamp before) {
        if (schema.isVirtual()) {
            return;
        }

        Period dataSourceLevelTTL = schema.getTtl();
        if (dataSourceLevelTTL != null && dataSourceLevelTTL.getMilliseconds() != 0) {
            //use datasource ttl
            before = TimeSpan.now()
                             .floor(Duration.ofMinutes(1))
                             .before(dataSourceLevelTTL.getMilliseconds(), TimeUnit.MILLISECONDS)
                             .toTimestamp();
        }

        log.info("\tClean up [{}] before {}", schema.getName(), DateTime.toYYYYMMDDhhmmss(before));
        try {
            expireImpl(schema, before);
        } catch (Exception e) {
            log.error("Failed to clean " + schema.getName(), e);
        }
    }

    protected abstract void expireImpl(DataSourceSchema schema, Timestamp before);
}
