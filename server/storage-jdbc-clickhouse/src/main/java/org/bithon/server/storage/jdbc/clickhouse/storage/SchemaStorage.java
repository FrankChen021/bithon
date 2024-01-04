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

package org.bithon.server.storage.jdbc.clickhouse.storage;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.security.HashGenerator;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageConfiguration;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.jdbc.meta.SchemaJdbcStorage;
import org.jooq.Record;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 7/1/22 3:06 PM
 */
@JsonTypeName("clickhouse")
public class SchemaStorage extends SchemaJdbcStorage {

    private final ClickHouseConfig config;

    @JsonCreator
    public SchemaStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseStorageConfiguration configuration,
                         @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper) {
        super(configuration.getDslContext(), objectMapper);
        this.config = configuration.getClickHouseConfig();
    }

    @Override
    public void initialize() {
        new TableCreator(config, dslContext).useReplacingMergeTree(Tables.BITHON_META_SCHEMA.TIMESTAMP.getName())
                                            .partitionByExpression(null)
                                            .secondaryIndex(Tables.BITHON_META_SCHEMA.TIMESTAMP.getName(), new TableCreator.SecondaryIndex("minmax", 128))
                                            .createIfNotExist(Tables.BITHON_META_SCHEMA);
    }

    @Override
    public List<DataSourceSchema> getSchemas(long afterTimestamp) {
        String sql = dslContext.select(Tables.BITHON_META_SCHEMA.NAME,
                                       Tables.BITHON_META_SCHEMA.SCHEMA,
                                       Tables.BITHON_META_SCHEMA.SIGNATURE)
                               .from(Tables.BITHON_META_SCHEMA)
                               .getSQL() + " FINAL WHERE ";
        sql += dslContext.renderInlined(Tables.BITHON_META_SCHEMA.TIMESTAMP.ge(new Timestamp(afterTimestamp).toLocalDateTime()));

        List<Record> records = dslContext.fetch(sql);
        if (records == null) {
            return Collections.emptyList();
        }

        return records.stream().map((mapper) -> {
            String name = mapper.get(0, String.class);
            String schema = mapper.get(1, String.class);
            String signature = mapper.get(2, String.class);
            return toSchema(name, schema, signature);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    @Override
    public List<DataSourceSchema> getSchemas() {
        String sql = dslContext.select(Tables.BITHON_META_SCHEMA.NAME,
                                       Tables.BITHON_META_SCHEMA.SCHEMA,
                                       Tables.BITHON_META_SCHEMA.SIGNATURE).from(Tables.BITHON_META_SCHEMA).getSQL() + " FINAL";

        List<Record> records = dslContext.fetch(sql);
        if (records == null) {
            return Collections.emptyList();
        }

        return records.stream()
                      .map(record -> toSchema(record.get(0, String.class),
                                              record.get(1, String.class),
                                              record.get(2, String.class)))
                      .filter(Objects::nonNull)
                      .collect(Collectors.toList());
    }

    @Override
    public DataSourceSchema getSchemaByName(String name) {
        String sql = dslContext.select(Tables.BITHON_META_SCHEMA.SCHEMA,
                                       Tables.BITHON_META_SCHEMA.SIGNATURE).from(Tables.BITHON_META_SCHEMA).getSQL()
                     + " FINAL where "
                     + Tables.BITHON_META_SCHEMA.NAME.eq(name).toString();

        Record record = dslContext.fetchOne(sql);
        return record == null ? null : toSchema(name, record.get(0, String.class), record.get(1, String.class));
    }

    /**
     * clickhouse does not support update, as we're using replacing merge tree, just to insert one record
     */
    @Override
    public void update(String name, DataSourceSchema schema) throws IOException {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        String schemaText = objectMapper.writeValueAsString(schema);
        schema.setSignature(HashGenerator.sha256Hex(schemaText));

        dslContext.insertInto(Tables.BITHON_META_SCHEMA)
                  .set(Tables.BITHON_META_SCHEMA.NAME, name)
                  .set(Tables.BITHON_META_SCHEMA.SCHEMA, schemaText)
                  .set(Tables.BITHON_META_SCHEMA.SIGNATURE, schema.getSignature())
                  .set(Tables.BITHON_META_SCHEMA.TIMESTAMP, now.toLocalDateTime())
                  .execute();
    }

    @Override
    public void putIfNotExist(String name, DataSourceSchema schema) throws IOException {
        String schemaText = objectMapper.writeValueAsString(schema);
        schema.setSignature(HashGenerator.sha256Hex(schemaText));

        if (dslContext.fetchCount(Tables.BITHON_META_SCHEMA, Tables.BITHON_META_SCHEMA.NAME.eq(name)) > 0) {
            return;
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        dslContext.insertInto(Tables.BITHON_META_SCHEMA)
                  .set(Tables.BITHON_META_SCHEMA.NAME, name)
                  .set(Tables.BITHON_META_SCHEMA.SCHEMA, schemaText)
                  .set(Tables.BITHON_META_SCHEMA.SIGNATURE, schema.getSignature())
                  .set(Tables.BITHON_META_SCHEMA.TIMESTAMP, now.toLocalDateTime())
                  .execute();
    }

    @Override
    public void putIfNotExist(String name, String schemaText) {
        if (dslContext.fetchCount(Tables.BITHON_META_SCHEMA, Tables.BITHON_META_SCHEMA.NAME.eq(name)) > 0) {
            return;
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        dslContext.insertInto(Tables.BITHON_META_SCHEMA)
                  .set(Tables.BITHON_META_SCHEMA.NAME, name)
                  .set(Tables.BITHON_META_SCHEMA.SCHEMA, schemaText)
                  .set(Tables.BITHON_META_SCHEMA.SIGNATURE, HashGenerator.sha256Hex(schemaText))
                  .set(Tables.BITHON_META_SCHEMA.TIMESTAMP, now.toLocalDateTime())
                  .execute();
    }
}
