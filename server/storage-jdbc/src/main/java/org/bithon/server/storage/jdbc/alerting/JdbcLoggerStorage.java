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

package org.bithon.server.storage.jdbc.alerting;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.server.storage.alerting.IEvaluationLogStorage;
import org.bithon.server.storage.alerting.IEvaluatorLogReader;
import org.bithon.server.storage.alerting.IEvaluatorLogWriter;
import org.bithon.server.storage.common.IStorageCleaner;
import org.bithon.server.storage.jdbc.JdbcJooqContextHolder;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.jooq.DSLContext;

/**
 * @author Frank Chen
 * @date 19/3/22 12:49 PM
 */
@JsonTypeName("jdbc")
public class JdbcLoggerStorage implements IEvaluationLogStorage {
    protected final DSLContext dslContext;

    @JsonCreator
    public JdbcLoggerStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcJooqContextHolder dslContextHolder) {
        this(dslContextHolder.getDslContext());
    }

    public JdbcLoggerStorage(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public void initialize() {
        dslContext.createTableIfNotExists(Tables.BITHON_ALERT_RUNLOG)
                  .columns(Tables.BITHON_ALERT_RUNLOG.fields())
                  .indexes(Tables.BITHON_ALERT_RUNLOG.getIndexes())
                  .execute();
    }

    @Override
    public IEvaluatorLogWriter createWriter() {
        return new JdbcLogWriter(dslContext);
    }

    @Override
    public IEvaluatorLogReader createReader() {
        return new JdbcLogReader(dslContext);
    }

    @Override
    public IStorageCleaner createCleaner() {
        return before -> dslContext.deleteFrom(Tables.BITHON_ALERT_RUNLOG)
                                   .where(Tables.BITHON_ALERT_RUNLOG.TIMESTAMP.le(before))
                                   .execute();
    }
}
