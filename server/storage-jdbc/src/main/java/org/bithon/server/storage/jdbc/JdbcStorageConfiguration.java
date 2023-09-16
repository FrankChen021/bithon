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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import org.bithon.server.storage.provider.IStorageConfiguration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqProperties;

import java.util.Map;
import java.util.Properties;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/8/27 15:08
 */
@JsonTypeName("jdbc")
public class JdbcStorageConfiguration implements IStorageConfiguration {

    @Getter
    @JsonIgnore
    private final DSLContext dslContext;

    @JsonCreator
    public JdbcStorageConfiguration(@JsonProperty("props") Map<String, Object> props) {
        Properties properties = new Properties();
        props.forEach((k, v) -> properties.put("druid." + k, v));

        DruidDataSource dataSource = DruidDataSourceBuilder.create().build();
        dataSource.configFromPropety(properties);

        JooqAutoConfiguration autoConfiguration = new JooqAutoConfiguration();
        this.dslContext = DSL.using(new DefaultConfiguration()
                                        .set(autoConfiguration.dataSourceConnectionProvider(dataSource))
                                        .set(new JooqProperties().determineSqlDialect(dataSource))
                                        .set(autoConfiguration.jooqExceptionTranslatorExecuteListenerProvider()));
    }

    @Override
    public String getType() {
        return "jdbc";
    }
}
