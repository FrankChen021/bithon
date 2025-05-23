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

package org.bithon.server.storage.jdbc.meta;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.HashUtils;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.storage.datasource.ISchemaStorage;
import org.bithon.server.storage.jdbc.JdbcStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.meta.MetaStorageConfig;
import org.jooq.DSLContext;
import org.springframework.dao.DuplicateKeyException;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 7/1/22 1:44 PM
 */
@Slf4j
public class SchemaJdbcStorage implements ISchemaStorage {
    protected final DSLContext dslContext;
    protected final ObjectMapper objectMapper;
    protected final MetaStorageConfig storageConfig;

    @JsonCreator
    public SchemaJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcStorageProviderConfiguration providerConfiguration,
                             @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                             @JacksonInject(useInput = OptBoolean.FALSE) MetaStorageConfig storageConfig) {
        this(providerConfiguration.getDslContext(), objectMapper, storageConfig);
    }

    public SchemaJdbcStorage(DSLContext dslContext,
                             ObjectMapper objectMapper,
                             MetaStorageConfig storageConfig) {
        this.dslContext = dslContext;
        this.objectMapper = objectMapper;
        this.storageConfig = storageConfig;
    }

    @Override
    public void initialize() {
        if (!this.storageConfig.isCreateTable()) {
            return;
        }

        this.dslContext.createTableIfNotExists(Tables.BITHON_META_SCHEMA)
                       .columns(Tables.BITHON_META_SCHEMA.fields())
                       .indexes(Tables.BITHON_META_SCHEMA.getIndexes())
                       .execute();
    }

    @Override
    public List<ISchema> getSchemas(long afterTimestamp) {
        return dslContext.selectFrom(Tables.BITHON_META_SCHEMA)
                         .where(Tables.BITHON_META_SCHEMA.TIMESTAMP.ge(new Timestamp(afterTimestamp).toLocalDateTime()))
                         .fetch((record) -> toSchema(record.getName(), record.getSchema(), record.getSignature()))
                         .stream()
                         .filter(Objects::nonNull)
                         .collect(Collectors.toList());
    }

    @Override
    public boolean containsSchema(String name) {
        return dslContext.fetchCount(Tables.BITHON_META_SCHEMA,
                                     Tables.BITHON_META_SCHEMA.NAME.eq(name)) > 0;
    }

    @Override
    public List<ISchema> getSchemas() {
        return dslContext.selectFrom(Tables.BITHON_META_SCHEMA)
                         .fetch((record) -> toSchema(record.getName(), record.getSchema(), record.getSignature()))
                         .stream()
                         .filter(Objects::nonNull)
                         .collect(Collectors.toList());
    }

    @Override
    public ISchema getSchemaByName(String name) {
        return dslContext.selectFrom(Tables.BITHON_META_SCHEMA)
                         .where(Tables.BITHON_META_SCHEMA.NAME.eq(name))
                         .fetchOne((record) -> toSchema(name, record.getSchema(), record.getSignature()));
    }

    protected ISchema toSchema(String name, String schemaPayload, String hash) {
        try {
            ISchema schema = objectMapper.readValue(schemaPayload, ISchema.class);
            schema.setSignature(hash);
            return schema;
        } catch (JsonProcessingException e) {
            log.error("Error reading payload of schema [{}]: {}", name, e.getMessage());
            return null;
        }
    }

    @Override
    public void update(String name, ISchema schema) throws IOException {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        String schemaText = objectMapper.writeValueAsString(schema);
        schema.setSignature(HashUtils.sha256Hex(schemaText));
        try {
            dslContext.insertInto(Tables.BITHON_META_SCHEMA)
                      .set(Tables.BITHON_META_SCHEMA.NAME, name)
                      .set(Tables.BITHON_META_SCHEMA.SCHEMA, schemaText)
                      .set(Tables.BITHON_META_SCHEMA.SIGNATURE, schema.getSignature())
                      .set(Tables.BITHON_META_SCHEMA.TIMESTAMP, now.toLocalDateTime())
                      .execute();
        } catch (DuplicateKeyException e) {
            dslContext.update(Tables.BITHON_META_SCHEMA)
                      .set(Tables.BITHON_META_SCHEMA.SCHEMA, schemaText)
                      .set(Tables.BITHON_META_SCHEMA.TIMESTAMP, now.toLocalDateTime())
                      .set(Tables.BITHON_META_SCHEMA.SIGNATURE, schema.getSignature())
                      .where(Tables.BITHON_META_SCHEMA.NAME.eq(name))
                      .execute();
        }
    }

    @Override
    public void putIfNotExist(String name, ISchema schema) throws IOException {
        String schemaText = objectMapper.writeValueAsString(schema);

        schema.setSignature(HashUtils.sha256Hex(schemaText));

        // onDuplicateKeyIgnore is not supported on all DB
        // use try-catch instead
        try {
            dslContext.insertInto(Tables.BITHON_META_SCHEMA)
                      .set(Tables.BITHON_META_SCHEMA.NAME, name)
                      .set(Tables.BITHON_META_SCHEMA.SCHEMA, schemaText)
                      .set(Tables.BITHON_META_SCHEMA.SIGNATURE, schema.getSignature())
                      .set(Tables.BITHON_META_SCHEMA.TIMESTAMP, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                      .execute();
        } catch (DuplicateKeyException ignored) {
        }
    }

    @Override
    public void putIfNotExist(String name, String schemaText) {
        // onDuplicateKeyIgnore is not supported on all DB
        // use try-catch instead
        try {
            dslContext.insertInto(Tables.BITHON_META_SCHEMA)
                      .set(Tables.BITHON_META_SCHEMA.NAME, name)
                      .set(Tables.BITHON_META_SCHEMA.SCHEMA, schemaText)
                      .set(Tables.BITHON_META_SCHEMA.SIGNATURE, HashUtils.sha256Hex(schemaText))
                      .set(Tables.BITHON_META_SCHEMA.TIMESTAMP, new Timestamp(System.currentTimeMillis()).toLocalDateTime())
                      .execute();
        } catch (DuplicateKeyException ignored) {
        }
    }
}
