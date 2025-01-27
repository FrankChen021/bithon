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

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.server.storage.dashboard.DashboardStorageConfig;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.jdbc.dashboard.DashboardJdbcStorage;
import org.jooq.CreateTableElementListStep;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.BuiltInDataType;
import org.jooq.impl.SQLDataType;

/**
 * @author frank.chen021@outlook.com
 * @date 13/9/24 5:29 pm
 */
@JsonTypeName("mysql")
public class DashboardMySqlStorage extends DashboardJdbcStorage {

    @JsonCreator
    public DashboardMySqlStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcStorageProviderConfiguration configuration,
                                 @JacksonInject(useInput = OptBoolean.FALSE) DashboardStorageConfig storageConfig) {
        super(configuration.getDslContext(), storageConfig);
    }

    @Override
    public void initialize() {
        // Use mediumtext for payload column
        CreateTableElementListStep step = this.dslContext.createTableIfNotExists(Tables.BITHON_WEB_DASHBOARD);
        if (this.dslContext.dialect().equals(SQLDialect.MYSQL)) {
            for (Field<?> field : Tables.BITHON_WEB_DASHBOARD.fields()) {
                if (field.getName().equals(Tables.BITHON_WEB_DASHBOARD.PAYLOAD.getName())) {
                    step = step.column(field, new BuiltInDataType<>(SQLDialect.MYSQL, SQLDataType.CLOB, "mediumtext", "char"));
                } else {
                    step = step.column(field, field.getDataType());
                }
            }
        } else {
            step = step.columns(Tables.BITHON_WEB_DASHBOARD.fields());
        }

        step.indexes(Tables.BITHON_WEB_DASHBOARD.getIndexes())
            .execute();
    }
}
