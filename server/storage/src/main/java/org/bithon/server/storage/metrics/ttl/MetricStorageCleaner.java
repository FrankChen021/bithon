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
import org.bithon.server.datasource.ISchema;
import org.bithon.server.storage.common.expiration.IExpirationRunnable;
import org.bithon.server.storage.datasource.SchemaManager;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/11 17:15
 */
@Slf4j
public abstract class MetricStorageCleaner implements IExpirationRunnable {

    protected abstract SchemaManager getSchemaManager();

    @Override
    public void expire(Timestamp before) {
        List<TimeSpan> skipDateList = getSkipDateList();

        for (ISchema schema : getSchemaManager().getSchemas().values()) {
            expire(schema, before, skipDateList);
        }
    }

    private void expire(ISchema schema, Timestamp before, List<TimeSpan> skipDateList) {
        if (!schema.getDataStoreSpec().isInternal()) {
            return;
        }

        Period ttl = schema.getTtl();
        if (ttl != null && ttl.getMilliseconds() != 0) {
            // Use datasource ttl to override the global TTL
            before = TimeSpan.now()
                             .floor(Duration.ofMinutes(1))
                             .before(ttl.getMilliseconds(), TimeUnit.MILLISECONDS)
                             .toTimestamp();
        }

        log.info("\tClean up [{}] before {}", schema.getName(), DateTime.toYYYYMMDDhhmmss(before));
        try {
            expireImpl(schema, before, skipDateList);
        } catch (Exception e) {
            log.error("Failed to clean " + schema.getName(), e);
        }
    }

    protected List<TimeSpan> getSkipDateList() {
        return Collections.emptyList();
    }

    protected abstract void expireImpl(ISchema schema, Timestamp before, List<TimeSpan> skipDateList);
}
