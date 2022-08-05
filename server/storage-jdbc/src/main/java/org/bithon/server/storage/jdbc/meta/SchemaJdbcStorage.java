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
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.security.HashGenerator;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.jdbc.JdbcJooqContextHolder;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.meta.ISchemaStorage;
import org.jooq.DSLContext;
import org.springframework.dao.DuplicateKeyException;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 7/1/22 1:44 PM
 */
@Slf4j
@JsonTypeName("jdbc")
public class SchemaJdbcStorage implements ISchemaStorage {
    protected final DSLContext dslContext;
    protected final ObjectMapper objectMapper;

    @JsonCreator
    public SchemaJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcJooqContextHolder dslContextHolder,
                             @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper) {
        this(dslContextHolder.getDslContext(), objectMapper);
    }

    public SchemaJdbcStorage(DSLContext dslContext,
                             ObjectMapper objectMapper) {
        this.dslContext = dslContext;
        this.objectMapper = objectMapper;
    }

    @Override
    public void initialize() {
        this.dslContext.createTableIfNotExists(Tables.BITHON_META_SCHEMA)
                       .columns(Tables.BITHON_META_SCHEMA.fields())
                       .indexes(Tables.BITHON_META_SCHEMA.getIndexes())
                       .execute();
    }

    @Override
    public List<DataSourceSchema> getSchemas(long afterTimestamp) {
        return dslContext.selectFrom(Tables.BITHON_META_SCHEMA)
                         .where(Tables.BITHON_META_SCHEMA.TIMESTAMP.ge(new Timestamp(afterTimestamp)))
                         .fetch((record) -> toSchema(record.getName(), record.getSchema(), record.getSignature()))
                         .stream()
                         .filter(Objects::nonNull)
                         .collect(Collectors.toList());
    }

    @Override
    public boolean containsSchema(String name) {
        // fetchExists API generates SQL as
        // select 1 "one" where exists ( select ... from ... where name = '')
        // such SQL is not supported by ClickHouse
        return dslContext.fetchCount(dslContext.select(Tables.BITHON_META_SCHEMA.NAME)
                                               .from(Tables.BITHON_META_SCHEMA)
                                               .where(Tables.BITHON_META_SCHEMA.NAME.eq(name))) > 0;
    }

    @Override
    public List<DataSourceSchema> getSchemas() {
        return dslContext.selectFrom(Tables.BITHON_META_SCHEMA)
                         .fetch((record) -> toSchema(record.getName(), record.getSchema(), record.getSignature()))
                         .stream()
                         .filter(Objects::nonNull)
                         .collect(Collectors.toList());
    }

    @Override
    public DataSourceSchema getSchemaByName(String name) {
        return dslContext.selectFrom(Tables.BITHON_META_SCHEMA)
                         .where(Tables.BITHON_META_SCHEMA.NAME.eq(name))
                         .fetchOne((record) -> toSchema(name, record.getSchema(), record.getSignature()));
    }

    protected DataSourceSchema toSchema(String name, String schemaPayload, String hash) {
        try {
            DataSourceSchema schema = objectMapper.readValue(schemaPayload, DataSourceSchema.class);
            schema.setSignature(hash);
            return schema;
        } catch (JsonProcessingException e) {
            log.error(StringUtils.format("Error reading payload of schema [%s].", name), e);
            return null;
        }
    }

    @Override
    public void update(String name, DataSourceSchema schema) throws IOException {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        String schemaText = objectMapper.writeValueAsString(schema);
        schema.setSignature(HashGenerator.sha256Hex(schemaText));
        try {
            dslContext.insertInto(Tables.BITHON_META_SCHEMA)
                      .set(Tables.BITHON_META_SCHEMA.NAME, name)
                      .set(Tables.BITHON_META_SCHEMA.SCHEMA, schemaText)
                      .set(Tables.BITHON_META_SCHEMA.SIGNATURE, schema.getSignature())
                      .set(Tables.BITHON_META_SCHEMA.TIMESTAMP, now)
                      .execute();
        } catch (DuplicateKeyException e) {
            dslContext.update(Tables.BITHON_META_SCHEMA)
                      .set(Tables.BITHON_META_SCHEMA.SCHEMA, schemaText)
                      .set(Tables.BITHON_META_SCHEMA.TIMESTAMP, now)
                      .set(Tables.BITHON_META_SCHEMA.SIGNATURE, schema.getSignature())
                      .where(Tables.BITHON_META_SCHEMA.NAME.eq(name))
                      .execute();
        }
    }

    @Override
    public void putIfNotExist(String name, DataSourceSchema schema) throws IOException {
        String schemaText = objectMapper.writeValueAsString(schema);

        schema.setSignature(HashGenerator.sha256Hex(schemaText));

        // onDuplicateKeyIgnore is not supported on all DB
        // use try-catch instead
        try {
            dslContext.insertInto(Tables.BITHON_META_SCHEMA)
                      .set(Tables.BITHON_META_SCHEMA.NAME, name)
                      .set(Tables.BITHON_META_SCHEMA.SCHEMA, schemaText)
                      .set(Tables.BITHON_META_SCHEMA.SIGNATURE, schema.getSignature())
                      .set(Tables.BITHON_META_SCHEMA.TIMESTAMP, new Timestamp(System.currentTimeMillis()))
                      .execute();
        } catch (DuplicateKeyException ignored) {
        }
    }
}
