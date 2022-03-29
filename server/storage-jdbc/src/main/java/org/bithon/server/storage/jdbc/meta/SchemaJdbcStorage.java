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
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.server.metric.DataSourceSchema;
import org.bithon.server.metric.storage.ISchemaStorage;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.jdbc.jooq.tables.records.BithonMetaSchemaRecord;
import org.jooq.DSLContext;
import org.springframework.dao.DuplicateKeyException;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.bithon.server.storage.jdbc.JdbcStorageAutoConfiguration.BITHON_JDBC_DSL;

/**
 * @author Frank Chen
 * @date 7/1/22 1:44 PM
 */
@JsonTypeName("jdbc")
public class SchemaJdbcStorage implements ISchemaStorage {
    protected final DSLContext dslContext;
    protected final ObjectMapper objectMapper;

    public SchemaJdbcStorage(@JacksonInject(value = BITHON_JDBC_DSL, useInput = OptBoolean.FALSE) DSLContext dslContext,
                             @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper) {
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
    public List<DataSourceSchema> getSchemas() {
        return dslContext.selectFrom(Tables.BITHON_META_SCHEMA)
                         .fetch(this::toSchema)
                         .stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public DataSourceSchema getSchemaByName(String name) {
        return dslContext.selectFrom(Tables.BITHON_META_SCHEMA)
                         .where(Tables.BITHON_META_SCHEMA.NAME.eq(name))
                         .fetchOne(this::toSchema);
    }

    protected DataSourceSchema toSchema(BithonMetaSchemaRecord r) {
        try {
            return objectMapper.readValue(r.getSchema(), DataSourceSchema.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public void update(String name, DataSourceSchema schema) throws IOException {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        String schemaText = objectMapper.writeValueAsString(schema);
        try {
            dslContext.insertInto(Tables.BITHON_META_SCHEMA)
                      .set(Tables.BITHON_META_SCHEMA.NAME, name)
                      .set(Tables.BITHON_META_SCHEMA.SCHEMA, schemaText)
                      .set(Tables.BITHON_META_SCHEMA.TIMESTAMP, now)
                      .execute();
        } catch (DuplicateKeyException e) {
            dslContext.update(Tables.BITHON_META_SCHEMA)
                      .set(Tables.BITHON_META_SCHEMA.SCHEMA, schemaText)
                      .set(Tables.BITHON_META_SCHEMA.TIMESTAMP, now)
                      .where(Tables.BITHON_META_SCHEMA.NAME.eq(name))
                      .execute();
        }
    }

    @Override
    public void putIfNotExist(String name, DataSourceSchema schema) throws IOException {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        String schemaText = objectMapper.writeValueAsString(schema);
        try {
            dslContext.insertInto(Tables.BITHON_META_SCHEMA)
                      .set(Tables.BITHON_META_SCHEMA.NAME, name)
                      .set(Tables.BITHON_META_SCHEMA.SCHEMA, schemaText)
                      .set(Tables.BITHON_META_SCHEMA.TIMESTAMP, now)
                      .execute();
        } catch (DuplicateKeyException ignored) {
        }
    }
}
