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

package org.bithon.server.storage.jdbc.postgresql.meta;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.jdbc.meta.MetadataJdbcStorage;
import org.bithon.server.storage.jdbc.postgresql.TableCreator;
import org.bithon.server.storage.meta.MetaStorageConfig;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/12/29 23:28
 */
public class MetadataStorage extends MetadataJdbcStorage {
    @JsonCreator
    public MetadataStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcStorageProviderConfiguration providerConfiguration,
                           @JacksonInject(useInput = OptBoolean.FALSE) MetaStorageConfig metaStorageConfig
    ) {
        super(providerConfiguration.getDslContext(), metaStorageConfig);
    }

    @Override
    public void initialize() {
        TableCreator.createTableIfNotExists(this.dslContext, Tables.BITHON_APPLICATION_INSTANCE);
    }
}
