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

package org.bithon.server.storage.jdbc.mysql;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.alerting.AlertObjectJdbcStorage;
import org.bithon.server.storage.jdbc.alerting.AlertRecordJdbcStorage;
import org.bithon.server.storage.jdbc.alerting.EvaluationLogJdbcStorage;
import org.bithon.server.storage.jdbc.alerting.NotificationChannelJdbcStorage;
import org.bithon.server.storage.jdbc.event.EventJdbcStorage;
import org.bithon.server.storage.jdbc.meta.MetadataJdbcStorage;
import org.bithon.server.storage.jdbc.meta.SchemaJdbcStorage;
import org.bithon.server.storage.jdbc.metric.MetricJdbcStorage;
import org.bithon.server.storage.jdbc.setting.SettingJdbcStorage;
import org.bithon.server.storage.jdbc.tracing.TraceJdbcStorage;
import org.bithon.server.storage.jdbc.web.DashboardJdbcStorage;
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
public class MySQLStorageModuleAutoConfiguration {
    @Bean
    public Module mySQLStorageModel() {
        return new Module() {
            @Override
            public String getModuleName() {
                return "storage-jdbc-mysql";
            }

            @Override
            public Version version() {
                return Version.unknownVersion();
            }

            @Override
            public void setupModule(SetupContext context) {
                context.registerSubtypes(MySQLSqlDialect.class);

                context.registerSubtypes(new NamedType(JdbcStorageProviderConfiguration.class, "mysql"),

                                         // Allow reading external MySQL directly
                                         new NamedType(ExternalMySQLDataStoreSpec.class, "mysql"),

                                         new NamedType(MetricJdbcStorage.class, "mysql"),
                                         new NamedType(SettingJdbcStorage.class, "mysql"),
                                         new NamedType(TraceJdbcStorage.class, "mysql"),
                                         new NamedType(DashboardJdbcStorage.class, "mysql"),
                                         new NamedType(EventJdbcStorage.class, "mysql"),
                                         new NamedType(SchemaJdbcStorage.class, "mysql"),
                                         new NamedType(MetadataJdbcStorage.class, "mysql"),

                                         // Alerting
                                         new NamedType(EvaluationLogJdbcStorage.class, "mysql"),
                                         new NamedType(AlertObjectJdbcStorage.class, "mysql"),
                                         new NamedType(AlertRecordJdbcStorage.class, "mysql"),
                                         new NamedType(NotificationChannelJdbcStorage.class, "mysql")
                                         );
            }
        };
    }
}
