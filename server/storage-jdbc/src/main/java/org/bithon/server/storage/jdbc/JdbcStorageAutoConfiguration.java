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

package org.bithon.server.storage.jdbc;

import com.alibaba.druid.pool.DruidDataSource;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import org.bithon.server.storage.jdbc.event.EventJdbcStorage;
import org.bithon.server.storage.jdbc.meta.MetadataJdbcStorage;
import org.bithon.server.storage.jdbc.meta.SchemaJdbcStorage;
import org.bithon.server.storage.jdbc.metric.MetricJdbcStorage;
import org.bithon.server.storage.jdbc.setting.SettingJdbcStorage;
import org.bithon.server.storage.jdbc.tracing.TraceJdbcStorage;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * @author frank.chen021@outlook.com
 * @date 25/10/21 9:45 pm
 */
@Configuration
@ConditionalOnProperty(prefix = "bithon.storage.providers.jdbc", name = "enabled", havingValue = "true")
@AutoConfigureBefore({DataSourceAutoConfiguration.class})
public class JdbcStorageAutoConfiguration {

    @Primary
    @Bean("bithon-jdbc-dataSource")
    @ConfigurationProperties(prefix = "bithon.storage.providers.jdbc")
    DataSource createDataSource() {
        return new DruidDataSource();
    }

    @Bean
    JdbcJooqContextHolder jdbcJooqContextHolder(@Qualifier("bithon-jdbc-dataSource") DataSource dataSource) {
        JooqAutoConfiguration autoConfiguration = new JooqAutoConfiguration();
        return new JdbcJooqContextHolder(DSL.using(new DefaultConfiguration()
                                                       .set(autoConfiguration.dataSourceConnectionProvider(dataSource))
                                                       .set(new JooqProperties().determineSqlDialect(dataSource))
                                                       .set(autoConfiguration.jooqExceptionTranslatorExecuteListenerProvider())));
    }

    @Bean
    public Module jdbcStorageModel() {
        return new Module() {
            @Override
            public String getModuleName() {
                return "storage-jdbc";
            }

            @Override
            public Version version() {
                return Version.unknownVersion();
            }

            @Override
            public void setupModule(SetupContext context) {
                context.registerSubtypes(TraceJdbcStorage.class,
                                         MetricJdbcStorage.class,
                                         SchemaJdbcStorage.class,
                                         EventJdbcStorage.class,
                                         MetadataJdbcStorage.class,
                                         SettingJdbcStorage.class);
            }
        };
    }
}
