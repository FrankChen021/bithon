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

package org.bithon.server.storage.jdbc.h2;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.alerting.AlertObjectJdbcStorage;
import org.bithon.server.storage.jdbc.alerting.AlertRecordJdbcStorage;
import org.bithon.server.storage.jdbc.alerting.AlertStateJdbcStorage;
import org.bithon.server.storage.jdbc.alerting.EvaluationLogJdbcStorage;
import org.bithon.server.storage.jdbc.alerting.NotificationChannelJdbcStorage;
import org.bithon.server.storage.jdbc.dashboard.DashboardJdbcStorage;
import org.bithon.server.storage.jdbc.event.EventJdbcStorage;
import org.bithon.server.storage.jdbc.meta.MetadataJdbcStorage;
import org.bithon.server.storage.jdbc.meta.SchemaJdbcStorage;
import org.bithon.server.storage.jdbc.metric.MetricJdbcStorage;
import org.bithon.server.storage.jdbc.setting.SettingJdbcStorage;
import org.bithon.server.storage.jdbc.tracing.TraceJdbcStorage;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author frank.chen021@outlook.com
 * @date 25/10/21 9:45 pm
 */
@Configuration
@AutoConfigureBefore({DataSourceAutoConfiguration.class})
public class H2StorageModuleAutoConfiguration {

    @Bean
    public Module h2StorageModel() {
        return new Module() {
            @Override
            public String getModuleName() {
                return "storage-jdbc-h2";
            }

            @Override
            public Version version() {
                return Version.unknownVersion();
            }

            @Override
            public void setupModule(SetupContext context) {
                context.registerSubtypes(new NamedType(JdbcStorageProviderConfiguration.class, "h2"),

                                         new NamedType(MetricJdbcStorage.class, "h2"),
                                         new NamedType(SettingJdbcStorage.class, "h2"),
                                         new NamedType(TraceJdbcStorage.class, "h2"),
                                         new NamedType(DashboardJdbcStorage.class, "h2"),
                                         new NamedType(EventJdbcStorage.class, "h2"),
                                         new NamedType(SchemaJdbcStorage.class, "h2"),
                                         new NamedType(MetadataJdbcStorage.class, "h2"),

                                         // Alerting
                                         new NamedType(EvaluationLogJdbcStorage.class, "h2"),
                                         new NamedType(AlertObjectJdbcStorage.class, "h2"),
                                         new NamedType(AlertRecordJdbcStorage.class, "h2"),
                                         new NamedType(AlertStateJdbcStorage.class, "h2"),
                                         new NamedType(NotificationChannelJdbcStorage.class, "h2")
                );
            }
        };
    }
}
