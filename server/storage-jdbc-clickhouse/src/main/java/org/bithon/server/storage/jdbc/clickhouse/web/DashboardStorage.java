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

package org.bithon.server.storage.jdbc.clickhouse.web;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.component.commons.security.HashGenerator;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseJooqContextHolder;
import org.bithon.server.storage.jdbc.clickhouse.TableCreator;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.jdbc.web.JdbcDashboardStorage;
import org.bithon.server.storage.web.Dashboard;
import org.jooq.Record;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 19/8/22 2:16 pm
 */
@JsonTypeName("clickhouse")
public class DashboardStorage extends JdbcDashboardStorage {
    private final ClickHouseConfig config;

    @JsonCreator
    public DashboardStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseJooqContextHolder dslContextHolder,
                            @JacksonInject(useInput = OptBoolean.FALSE) ClickHouseConfig config) {
        super(dslContextHolder.getDslContext());
        this.config = config;
    }

    @Override
    public void initialize() {
        new TableCreator(config, dslContext).useReplacingMergeTree(true)
                                            .partitionByExpressioin(null)
                                            .createIfNotExist(Tables.BITHON_WEB_DASHBOARD);
    }

    @Override
    public List<Dashboard> getDashboard(long afterTimestamp) {
        String sql = dslContext.selectFrom(Tables.BITHON_WEB_DASHBOARD)
                               .getSQL() + " FINAL WHERE ";
        sql += dslContext.renderInlined(Tables.BITHON_WEB_DASHBOARD.TIMESTAMP.ge(new Timestamp(afterTimestamp)));

        List<Record> records = dslContext.fetch(sql);
        if (records == null) {
            return Collections.emptyList();
        }

        return records.stream().map(this::toDashboard).collect(Collectors.toList());
    }

    @Override
    public String put(String name, String payload) {
        String signature = HashGenerator.sha256Hex(payload);

        Timestamp now = new Timestamp(System.currentTimeMillis());
        dslContext.insertInto(Tables.BITHON_WEB_DASHBOARD)
                  .set(Tables.BITHON_WEB_DASHBOARD.NAME, name)
                  .set(Tables.BITHON_WEB_DASHBOARD.PAYLOAD, payload)
                  .set(Tables.BITHON_WEB_DASHBOARD.SIGNATURE, signature)
                  .set(Tables.BITHON_WEB_DASHBOARD.TIMESTAMP, now)
                  .set(Tables.BITHON_WEB_DASHBOARD.DELETED, 0)
                  .execute();

        return signature;
    }

    @Override
    public void putIfNotExist(String name, String payload) {
        String signature = HashGenerator.sha256Hex(payload);

        if (dslContext.fetchCount(Tables.BITHON_WEB_DASHBOARD,
                                  Tables.BITHON_WEB_DASHBOARD.NAME.eq(name)) > 0) {
            return;
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        dslContext.insertInto(Tables.BITHON_WEB_DASHBOARD)
                  .set(Tables.BITHON_WEB_DASHBOARD.NAME, name)
                  .set(Tables.BITHON_WEB_DASHBOARD.PAYLOAD, payload)
                  .set(Tables.BITHON_WEB_DASHBOARD.SIGNATURE, signature)
                  .set(Tables.BITHON_WEB_DASHBOARD.TIMESTAMP, now)
                  .set(Tables.BITHON_WEB_DASHBOARD.DELETED, 0)
                  .execute();
    }
}
