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

package org.bithon.server.storage.druid;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.zaxxer.hikari.HikariDataSource;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.StorageAutoConfiguration;
import org.bithon.server.storage.druid.web.DashboardStorage;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * @author frank.chen021@outlook.com
 * @date 27/10/21 9:45 pm
 */
@AutoConfigureBefore(StorageAutoConfiguration.class)
@ConditionalOnProperty(prefix = "bithon.storage.providers.druid", name = "enabled", havingValue = "true")
public class DruidStorageAutoConfiguration {

    @Bean("bithon-druid-dataSource")
    @ConfigurationProperties(prefix = "bithon.storage.providers.druid")
    DataSource createDataSource(DruidConfig config) {
        HikariDataSource dataSource = DataSourceBuilder.create().type(com.zaxxer.hikari.HikariDataSource.class).build();
        dataSource.setJdbcUrl(StringUtils.format("jdbc:avatica:remote:url=%s/druid/v2/sql/avatica/", config.getRouter()));
        return dataSource;
    }

    @Bean
    @ConfigurationProperties(prefix = "bithon.storage.providers.druid")
    DruidConfig druidConfig() {
        return new DruidConfig();
    }

    @Bean
    DruidSqlDialect druidSqlDialect() {
        return new DruidSqlDialect();
    }

    @Bean
    DruidJooqContextHolder druidDSLContextHolder(@Qualifier("bithon-druid-dataSource") DataSource dataSource) {
        JooqAutoConfiguration autoConfiguration = new JooqAutoConfiguration();
        return new DruidJooqContextHolder(DSL.using(new DefaultConfiguration().set(autoConfiguration.dataSourceConnectionProvider(dataSource))
                                                                              .set(new JooqProperties().determineSqlDialect(dataSource))
                                                                              .set(autoConfiguration.jooqExceptionTranslatorExecuteListenerProvider())));
    }

    @Bean
    public Module druidStorageModule() {
        return new Module() {
            @Override
            public String getModuleName() {
                return "storage-druid";
            }

            @Override
            public Version version() {
                return Version.unknownVersion();
            }

            @Override
            public void setupModule(SetupContext context) {
                context.registerSubtypes(MetricStorage.class,
                                         SchemaStorage.class,
                                         DashboardStorage.class);
            }
        };
    }
}
