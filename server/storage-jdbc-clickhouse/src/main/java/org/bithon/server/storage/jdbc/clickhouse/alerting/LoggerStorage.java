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

package org.bithon.server.storage.jdbc.clickhouse.alerting;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.component.commons.time.DateTime;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.common.IStorageCleaner;
import org.bithon.server.storage.jdbc.alerting.JdbcLoggerStorage;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseJooqContextHolder;
import org.bithon.server.storage.jdbc.clickhouse.TableCreator;
import org.bithon.server.storage.jdbc.jooq.Tables;

/**
 * @author Frank Chen
 * @date 19/3/22 12:49 PM
 */
@JsonTypeName("clickhouse")
public class LoggerStorage extends JdbcLoggerStorage {

    private final ClickHouseConfig config;

    @JsonCreator
    public LoggerStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseJooqContextHolder dslContextHolder,
                         @JacksonInject(useInput = OptBoolean.FALSE) ClickHouseConfig config) {
        super(dslContextHolder.getDslContext());

        this.config = config;
    }

    @Override
    public void initialize() {
        new TableCreator(this.config, this.dslContext).createIfNotExist(Tables.BITHON_ALERT_RUNLOG);
    }

    @Override
    public IStorageCleaner createCleaner() {
        return beforeTimestamp -> dslContext.execute(StringUtils.format("ALTER TABLE %s.%s %s DELETE WHERE timestamp < '%s'",
                                                                        config.getDatabase(),
                                                                        config.getLocalTableName(Tables.BITHON_ALERT_RUNLOG.getName()),
                                                                        config.getClusterExpression(),
                                                                        DateTime.toYYYYMMDDhhmmss(beforeTimestamp)));
    }
}
