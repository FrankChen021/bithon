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
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.Getter;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.datasource.reader.jdbc.dialect.SqlDialectManager;
import org.bithon.server.storage.InvalidConfigurationException;
import org.bithon.server.storage.common.provider.IStorageProviderConfiguration;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.tools.jdbc.JDBCUtils;
import org.springframework.boot.autoconfigure.jooq.ExceptionTranslatorExecuteListener;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;

import java.util.Map;
import java.util.Properties;

/**
 * Should be registered to ObjectMapper in each implementation.
 * Can take the H2StorageModuleAutoConfiguration for reference.
 *
 * @author frank.chen021@outlook.com
 * @date 2023/8/27 15:08
 */
public class JdbcStorageProviderConfiguration implements IStorageProviderConfiguration {

    @Getter
    @JsonIgnore
    private final DSLContext dslContext;

    @JsonCreator
    public JdbcStorageProviderConfiguration(@JsonProperty("props") Map<String, Object> props,
                                            @JacksonInject(useInput = OptBoolean.FALSE) SqlDialectManager sqlDialectManager) {
        String url = ((String) props.getOrDefault("url", "")).trim();
        InvalidConfigurationException.throwIf(url.isEmpty(), "url property is missed.");
        InvalidConfigurationException.throwIf(!url.startsWith("jdbc:"), StringUtils.format("The 'url' property [%s] should start with 'jdbc:'.", props.get("url")));

        Properties properties = new Properties();
        props.forEach((k, v) -> properties.put("druid." + k, v));

        if (!properties.contains("druid.maxWait")) {
            properties.put("druid.maxWait", "10000");
        }

        // Inference the SQL dialect from the URL first
        SQLDialect sqlDialect = JDBCUtils.dialect(url);
        InvalidConfigurationException.throwIf(sqlDialect.equals(SQLDialect.DEFAULT), StringUtils.format("Unknown SQL dialect from the given url: %s", url));

        // Check if the dialect is supported
        sqlDialectManager.getSqlDialect(sqlDialect);

        DruidDataSource dataSource = DruidDataSourceBuilder.create().build();
        dataSource.configFromPropety(properties);
        JooqAutoConfiguration autoConfiguration = new JooqAutoConfiguration();
        this.dslContext = DSL.using(new DefaultConfiguration()
                                        .set(autoConfiguration.dataSourceConnectionProvider(dataSource))
                                        .set(sqlDialect)
                                        .set(autoConfiguration.jooqExceptionTranslatorExecuteListenerProvider(ExceptionTranslatorExecuteListener.DEFAULT)));
    }
}
