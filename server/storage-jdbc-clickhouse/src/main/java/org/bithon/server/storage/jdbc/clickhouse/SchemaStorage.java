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

package org.bithon.server.storage.jdbc.clickhouse;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.server.metric.DataSourceSchema;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.jdbc.meta.SchemaJdbcStorage;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageAutoConfiguration.BITHON_CLICKHOUSE_DSL;

/**
 * @author Frank Chen
 * @date 7/1/22 3:06 PM
 */
@JsonTypeName("clickhouse")
public class SchemaStorage extends SchemaJdbcStorage {

    private final ClickHouseConfig config;

    @JsonCreator
    public SchemaStorage(@JacksonInject(value = BITHON_CLICKHOUSE_DSL, useInput = OptBoolean.FALSE) DSLContext dslContext,
                         @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                         @JacksonInject(useInput = OptBoolean.FALSE) ClickHouseConfig config) {
        super(dslContext, objectMapper);
        this.config = config;
    }

    @Override
    public void initialize() {
        new TableCreator(config, dslContext).createIfNotExist(Tables.BITHON_META_SCHEMA, -1, true, false);
    }

    @Override
    public List<DataSourceSchema> getSchemas() {
        String sql = dslContext.select(Tables.BITHON_META_SCHEMA.NAME, Tables.BITHON_META_SCHEMA.SCHEMA).from(Tables.BITHON_META_SCHEMA).getSQL() + " FINAL";

        List<Record> records = dslContext.fetch(sql);
        if (records == null) {
            return Collections.emptyList();
        }

        return records.stream().map((mapper) -> {
            try {
                return objectMapper.readValue(mapper.getValue(1, String.class), DataSourceSchema.class);
            } catch (JsonProcessingException e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public DataSourceSchema getSchemaByName(String name) {
        String sql = dslContext.select(Tables.BITHON_META_SCHEMA.NAME, Tables.BITHON_META_SCHEMA.SCHEMA).from(Tables.BITHON_META_SCHEMA).getSQL()
                     + " FINAL where "
                     + Tables.BITHON_META_SCHEMA.NAME.eq(name).toString();

        Record record = dslContext.fetchOne(sql);
        return record == null ? null : record.map((mapper) -> {
            try {
                return objectMapper.readValue(mapper.getValue(1, String.class), DataSourceSchema.class);
            } catch (JsonProcessingException e) {
                return null;
            }
        });
    }

    /**
     * clickhouse does not support update, as we're using replacing merge tree, just to insert one record
     */
    @Override
    public void update(String name, DataSourceSchema schema) throws IOException {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        String schemaText = objectMapper.writeValueAsString(schema);
        dslContext.insertInto(Tables.BITHON_META_SCHEMA)
                  .set(Tables.BITHON_META_SCHEMA.NAME, name)
                  .set(Tables.BITHON_META_SCHEMA.SCHEMA, schemaText)
                  .set(Tables.BITHON_META_SCHEMA.TIMESTAMP, now)
                  .execute();
    }

    @Override
    public void putIfNotExist(String name, DataSourceSchema schema) throws IOException {
        if (dslContext.fetchCount(Tables.BITHON_META_SCHEMA, Tables.BITHON_META_SCHEMA.NAME.eq(name)) > 0) {
            return;
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        String schemaText = objectMapper.writeValueAsString(schema);
        dslContext.insertInto(Tables.BITHON_META_SCHEMA)
                  .set(Tables.BITHON_META_SCHEMA.NAME, name)
                  .set(Tables.BITHON_META_SCHEMA.SCHEMA, schemaText)
                  .set(Tables.BITHON_META_SCHEMA.TIMESTAMP, now)
                  .execute();

    }
}
