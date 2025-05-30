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

package org.bithon.server.storage.jdbc.clickhouse;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import org.bithon.server.storage.jdbc.clickhouse.alerting.AlertObjectStorage;
import org.bithon.server.storage.jdbc.clickhouse.alerting.AlertRecordStorage;
import org.bithon.server.storage.jdbc.clickhouse.alerting.AlertStateStorage;
import org.bithon.server.storage.jdbc.clickhouse.alerting.EvaluationLogStorage;
import org.bithon.server.storage.jdbc.clickhouse.alerting.NotificationChannelStorage;
import org.bithon.server.storage.jdbc.clickhouse.event.EventStorage;
import org.bithon.server.storage.jdbc.clickhouse.meta.MetadataStorage;
import org.bithon.server.storage.jdbc.clickhouse.meta.SchemaStorage;
import org.bithon.server.storage.jdbc.clickhouse.metric.MetricStorage;
import org.bithon.server.storage.jdbc.clickhouse.setting.SettingStorage;
import org.bithon.server.storage.jdbc.clickhouse.trace.TraceStorage;
import org.bithon.server.storage.jdbc.clickhouse.web.DashboardStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author frank.chen021@outlook.com
 * @date 27/10/21 9:45 pm
 */
@Configuration
public class ClickHouseStorageAutoConfiguration {

    @Bean
    public Module clickHouseStorageModule() {
        return new Module() {
            @Override
            public String getModuleName() {
                return "storage-jdbc-clickhouse";
            }

            @Override
            public Version version() {
                return Version.unknownVersion();
            }

            @Override
            public void setupModule(SetupContext context) {
                context.registerSubtypes(ClickHouseStorageProviderConfiguration.class,
                                         TraceStorage.class,
                                         MetricStorage.class,
                                         EventStorage.class,
                                         MetadataStorage.class,
                                         SchemaStorage.class,
                                         SettingStorage.class,
                                         DashboardStorage.class,

                                         // alerting
                                         AlertObjectStorage.class,
                                         AlertRecordStorage.class,
                                         AlertStateStorage.class,
                                         EvaluationLogStorage.class,
                                         NotificationChannelStorage.class
                );
            }
        };
    }
}
