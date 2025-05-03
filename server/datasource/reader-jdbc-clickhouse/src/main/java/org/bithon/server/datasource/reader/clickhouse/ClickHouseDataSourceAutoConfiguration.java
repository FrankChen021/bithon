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

package org.bithon.server.datasource.reader.clickhouse;


import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author frank.chen021@outlook.com
 * @date 28/4/25 9:37 pm
 */
@Configuration
public class ClickHouseDataSourceAutoConfiguration {

    @Bean
    public Module clickHouseExternalDataSourceModule() {
        return new Module() {
            @Override
            public String getModuleName() {
                return "datasource-reader-jdbc-clickhouse";
            }

            @Override
            public Version version() {
                return Version.unknownVersion();
            }

            @Override
            public void setupModule(SetupContext context) {
                // Allow reading external ClickHouse directly
                context.registerSubtypes(new NamedType(ExternalClickHouseDataStoreSpec.class, "clickhouse"));
                // For backward compatibility
                context.registerSubtypes(new NamedType(ExternalClickHouseDataStoreSpec.class, "external"));

                context.registerSubtypes(new NamedType(ClickHouseSqlDialect.class, "clickhouse"));
                context.registerSubtypes(new NamedType(AggregateFunctionColumn.class, "aggregateFunction"));
            }
        };
    }
}
