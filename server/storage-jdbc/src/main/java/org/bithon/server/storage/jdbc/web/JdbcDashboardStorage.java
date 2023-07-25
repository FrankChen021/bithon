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

package org.bithon.server.storage.jdbc.web;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.component.commons.security.HashGenerator;
import org.bithon.server.storage.jdbc.JdbcJooqContextHolder;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.web.Dashboard;
import org.bithon.server.storage.web.IDashboardStorage;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.springframework.dao.DuplicateKeyException;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author Frank Chen
 * @date 19/8/22 12:41 pm
 */
@JsonTypeName("jdbc")
public class JdbcDashboardStorage implements IDashboardStorage {

    protected final DSLContext dslContext;

    @JsonCreator
    public JdbcDashboardStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcJooqContextHolder dslContextHolder) {
        this(dslContextHolder.getDslContext());
    }

    public JdbcDashboardStorage(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public List<Dashboard> getDashboard(long afterTimestamp) {
        return dslContext.selectFrom(Tables.BITHON_WEB_DASHBOARD)
                         .where(Tables.BITHON_WEB_DASHBOARD.TIMESTAMP.ge(new Timestamp(afterTimestamp).toLocalDateTime()))
                         .fetch(this::toDashboard);
    }

    @Override
    public String put(String name, String payload) {
        String signature = HashGenerator.sha256Hex(payload);

        // onDuplicateKeyIgnore is not supported on all DB
        // use try-catch instead
        try {
            dslContext.insertInto(Tables.BITHON_WEB_DASHBOARD)
                      .set(Tables.BITHON_WEB_DASHBOARD.NAME, name)
                      .set(Tables.BITHON_WEB_DASHBOARD.PAYLOAD, payload)
                      .set(Tables.BITHON_WEB_DASHBOARD.SIGNATURE, signature)
                      .set(Tables.BITHON_WEB_DASHBOARD.TIMESTAMP, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                      .set(Tables.BITHON_WEB_DASHBOARD.DELETED, 0)
                      .execute();
        } catch (DuplicateKeyException ignored) {
            // try to update if duplicated
            dslContext.update(Tables.BITHON_WEB_DASHBOARD)
                      .set(Tables.BITHON_WEB_DASHBOARD.PAYLOAD, payload)
                      .set(Tables.BITHON_WEB_DASHBOARD.SIGNATURE, signature)
                      .set(Tables.BITHON_WEB_DASHBOARD.TIMESTAMP, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                      .set(Tables.BITHON_WEB_DASHBOARD.DELETED, 0)
                      .where(Tables.BITHON_WEB_DASHBOARD.NAME.eq(name))
                      .execute();
        }
        return signature;
    }

    @Override
    public void putIfNotExist(String name, String payload) {
        String signature = HashGenerator.sha256Hex(payload);

        // onDuplicateKeyIgnore is not supported on all DB
        // use try-catch instead
        try {
            dslContext.insertInto(Tables.BITHON_WEB_DASHBOARD)
                      .set(Tables.BITHON_WEB_DASHBOARD.NAME, name)
                      .set(Tables.BITHON_WEB_DASHBOARD.PAYLOAD, payload)
                      .set(Tables.BITHON_WEB_DASHBOARD.SIGNATURE, signature)
                      .set(Tables.BITHON_WEB_DASHBOARD.TIMESTAMP, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                      .set(Tables.BITHON_WEB_DASHBOARD.DELETED, 0)
                      .execute();
        } catch (DuplicateKeyException ignored) {
        }
    }

    @Override
    public void initialize() {
        this.dslContext.createTableIfNotExists(Tables.BITHON_WEB_DASHBOARD)
                       .columns(Tables.BITHON_WEB_DASHBOARD.fields())
                       .indexes(Tables.BITHON_WEB_DASHBOARD.getIndexes())
                       .execute();
    }

    protected Dashboard toDashboard(Record record) {
        Dashboard dashboard = new Dashboard();
        dashboard.setName(record.get(Tables.BITHON_WEB_DASHBOARD.NAME));
        dashboard.setPayload(record.get(Tables.BITHON_WEB_DASHBOARD.PAYLOAD));
        dashboard.setTimestamp(Timestamp.valueOf(record.get(Tables.BITHON_WEB_DASHBOARD.TIMESTAMP)));
        dashboard.setSignature(record.get(Tables.BITHON_WEB_DASHBOARD.SIGNATURE));
        dashboard.setDeleted(record.get(Tables.BITHON_WEB_DASHBOARD.DELETED) == 1);
        return dashboard;
    }
}
