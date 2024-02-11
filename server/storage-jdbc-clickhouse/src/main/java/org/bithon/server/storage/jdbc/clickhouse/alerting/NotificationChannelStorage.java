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
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.alerting.pojo.NotificationChannelObject;
import org.bithon.server.storage.jdbc.alerting.NotificationChannelJdbcStorage;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.clickhouse.common.TableCreator;
import org.bithon.server.storage.jdbc.common.jooq.Tables;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/12/22 17:48
 */
@JsonTypeName("clickhouse")
public class NotificationChannelStorage extends NotificationChannelJdbcStorage {

    private final ClickHouseConfig clickHouseConfig;

    @JsonCreator
    public NotificationChannelStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseStorageProviderConfiguration provider) {
        super(provider.getDslContext());
        this.clickHouseConfig = provider.getClickHouseConfig();
    }

    @Override
    public List<NotificationChannelObject> getChannels(long since) {
        return super.getChannels(since);
    }

    @Override
    public void deleteChannel(String name) {
        String sql = StringUtils.format("ALTER TABLE %s DELETE WHERE %s = '%s'",
                                        Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.getName(),
                                        Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.NAME.getName(),
                                        name);
        dslContext.execute(sql);
    }

    @Override
    public void initialize() {
        new TableCreator(this.clickHouseConfig, this.dslContext)
            .partitionByExpression(null)
            .useReplacingMergeTree(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL.CREATED_AT.getName())
            .createIfNotExist(Tables.BITHON_ALERT_NOTIFICATION_CHANNEL);
    }
}
