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
import org.bithon.component.commons.utils.HashUtils;
import org.bithon.server.storage.dashboard.Dashboard;
import org.bithon.server.storage.dashboard.DashboardStorageConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.clickhouse.common.SecondaryIndex;
import org.bithon.server.storage.jdbc.clickhouse.common.TableCreator;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.jdbc.dashboard.DashboardJdbcStorage;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 19/8/22 2:16 pm
 */
@JsonTypeName("clickhouse")
public class DashboardStorage extends DashboardJdbcStorage {
    private final ClickHouseConfig config;

    @JsonCreator
    public DashboardStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseStorageProviderConfiguration configuration,
                            @JacksonInject(useInput = OptBoolean.FALSE) DashboardStorageConfig storageConfig) {
        super(configuration.getDslContext(), storageConfig);
        this.config = configuration.getClickHouseConfig();
    }

    @Override
    public void initialize() {
        if (!this.storageConfig.isCreateTable()) {
            return;
        }

        new TableCreator(config, dslContext).useReplacingMergeTree(Tables.BITHON_WEB_DASHBOARD.TIMESTAMP.getName())
                                            .partitionByExpression(null)
                                            .secondaryIndex(Tables.BITHON_WEB_DASHBOARD.TIMESTAMP.getName(), new SecondaryIndex("minmax", 512))
                                            .createIfNotExist(Tables.BITHON_WEB_DASHBOARD);
    }

    @Override
    public List<Dashboard> getDashboard(long afterTimestamp) {
        String sql = dslContext.selectFrom(Tables.BITHON_WEB_DASHBOARD)
                               .getSQL() + " FINAL WHERE ";
        sql += dslContext.renderInlined(Tables.BITHON_WEB_DASHBOARD.TIMESTAMP.ge(new Timestamp(afterTimestamp).toLocalDateTime()));

        return dslContext.fetch(sql)
                         .stream()
                         .map(this::toDashboard)
                         .collect(Collectors.toList());
    }

    @Override
    public String put(String name, String payload) {
        String signature = HashUtils.sha256Hex(payload);

        Timestamp now = new Timestamp(System.currentTimeMillis());
        dslContext.insertInto(Tables.BITHON_WEB_DASHBOARD)
                  .set(Tables.BITHON_WEB_DASHBOARD.NAME, name)
                  .set(Tables.BITHON_WEB_DASHBOARD.PAYLOAD, payload)
                  .set(Tables.BITHON_WEB_DASHBOARD.SIGNATURE, signature)
                  .set(Tables.BITHON_WEB_DASHBOARD.TIMESTAMP, now.toLocalDateTime())
                  .set(Tables.BITHON_WEB_DASHBOARD.DELETED, 0)
                  .execute();

        return signature;
    }

    @Override
    public void putIfNotExist(String name, String payload) {
        String signature = HashUtils.sha256Hex(payload);

        if (dslContext.fetchCount(Tables.BITHON_WEB_DASHBOARD,
                                  Tables.BITHON_WEB_DASHBOARD.NAME.eq(name)) > 0) {
            return;
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        dslContext.insertInto(Tables.BITHON_WEB_DASHBOARD)
                  .set(Tables.BITHON_WEB_DASHBOARD.NAME, name)
                  .set(Tables.BITHON_WEB_DASHBOARD.PAYLOAD, payload)
                  .set(Tables.BITHON_WEB_DASHBOARD.SIGNATURE, signature)
                  .set(Tables.BITHON_WEB_DASHBOARD.TIMESTAMP, now.toLocalDateTime())
                  .set(Tables.BITHON_WEB_DASHBOARD.DELETED, 0)
                  .execute();
    }
}
