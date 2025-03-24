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

package org.bithon.server.storage.jdbc.postgresql;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.postgresql.alerting.AlertObjectStorage;
import org.bithon.server.storage.jdbc.postgresql.alerting.AlertRecordStorage;
import org.bithon.server.storage.jdbc.postgresql.alerting.AlertStateStorage;
import org.bithon.server.storage.jdbc.postgresql.alerting.EvaluationLogStorage;
import org.bithon.server.storage.jdbc.postgresql.alerting.NotificationChannelStorage;
import org.bithon.server.storage.jdbc.postgresql.dashboard.DashboardStorage;
import org.bithon.server.storage.jdbc.postgresql.event.EventStorage;
import org.bithon.server.storage.jdbc.postgresql.meta.MetadataStorage;
import org.bithon.server.storage.jdbc.postgresql.meta.SchemaStorage;
import org.bithon.server.storage.jdbc.postgresql.metric.MetricStorage;
import org.bithon.server.storage.jdbc.postgresql.setting.SettingStorage;
import org.bithon.server.storage.jdbc.postgresql.trace.TraceStorage;
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
public class PostgresqlStorageModuleAutoConfiguration {

    @Bean
    public Module postgresqlStorageModel() {
        return new Module() {
            @Override
            public String getModuleName() {
                return "storage-jdbc-postgresql";
            }

            @Override
            public Version version() {
                return Version.unknownVersion();
            }

            @Override
            public void setupModule(SetupContext context) {
                context.registerSubtypes(PostgresqlDialect.class);

                context.registerSubtypes(new NamedType(JdbcStorageProviderConfiguration.class, "postgresql"),

                                         new NamedType(ExternalPostgresqlDataStoreSpec.class, "postgresql"),

                                         new NamedType(MetricStorage.class, "postgresql"),
                                         new NamedType(SettingStorage.class, "postgresql"),
                                         new NamedType(TraceStorage.class, "postgresql"),
                                         new NamedType(DashboardStorage.class, "postgresql"),
                                         new NamedType(EventStorage.class, "postgresql"),
                                         new NamedType(SchemaStorage.class, "postgresql"),
                                         new NamedType(MetadataStorage.class, "postgresql"),

                                         // Alerting
                                         new NamedType(EvaluationLogStorage.class, "postgresql"),
                                         new NamedType(AlertObjectStorage.class, "postgresql"),
                                         new NamedType(AlertRecordStorage.class, "postgresql"),
                                         new NamedType(AlertStateStorage.class, "postgresql"),
                                         new NamedType(NotificationChannelStorage.class, "postgresql")
                );
            }
        };
    }
}
