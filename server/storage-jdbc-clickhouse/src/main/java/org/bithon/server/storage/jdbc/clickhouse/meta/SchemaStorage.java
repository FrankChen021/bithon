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

package org.bithon.server.storage.jdbc.clickhouse.meta;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.utils.HashUtils;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseStorageProviderConfiguration;
import org.bithon.server.storage.jdbc.clickhouse.common.SecondaryIndex;
import org.bithon.server.storage.jdbc.clickhouse.common.TableCreator;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.jdbc.meta.SchemaJdbcStorage;
import org.bithon.server.storage.meta.MetaStorageConfig;
import org.jooq.Record;

import java.io.IOException;
import java.sql.Timestamp;
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
    public SchemaStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseStorageProviderConfiguration configuration,
                         @JacksonInject(useInput = OptBoolean.FALSE) ObjectMapper objectMapper,
                         @JacksonInject(useInput = OptBoolean.FALSE) MetaStorageConfig storageConfig) {
        super(configuration.getDslContext(), objectMapper, storageConfig);
        this.config = configuration.getClickHouseConfig();
    }

    @Override
    public void initialize() {
        if (!this.storageConfig.isCreateTable()) {
            return;
        }
        new TableCreator(config, dslContext).useReplacingMergeTree(Tables.BITHON_META_SCHEMA.TIMESTAMP.getName())
                                            .partitionByExpression(null)
                                            .secondaryIndex(Tables.BITHON_META_SCHEMA.TIMESTAMP.getName(), new SecondaryIndex("minmax", 128))
                                            .createIfNotExist(Tables.BITHON_META_SCHEMA);
    }

    @Override
    public List<ISchema> getSchemas(long afterTimestamp) {
        String sql = dslContext.select(Tables.BITHON_META_SCHEMA.NAME,
                                       Tables.BITHON_META_SCHEMA.SCHEMA,
                                       Tables.BITHON_META_SCHEMA.SIGNATURE)
                               .from(Tables.BITHON_META_SCHEMA)
                               .getSQL() + " FINAL WHERE ";
        sql += dslContext.renderInlined(Tables.BITHON_META_SCHEMA.TIMESTAMP.ge(new Timestamp(afterTimestamp).toLocalDateTime()));

        return dslContext.fetch(sql)
                         .stream()
                         .map((mapper) -> {
                             String name = mapper.get(0, String.class);
                             String schema = mapper.get(1, String.class);
                             String signature = mapper.get(2, String.class);
                             return toSchema(name, schema, signature);
                         })
                         .filter(Objects::nonNull)
                         .collect(Collectors.toList());
    }

    @Override
    public List<ISchema> getSchemas() {
        String sql = dslContext.select(Tables.BITHON_META_SCHEMA.NAME,
                                       Tables.BITHON_META_SCHEMA.SCHEMA,
                                       Tables.BITHON_META_SCHEMA.SIGNATURE).from(Tables.BITHON_META_SCHEMA).getSQL() + " FINAL";

        return dslContext.fetch(sql)
                         .stream()
                         .map(record -> toSchema(record.get(0, String.class),
                                                 record.get(1, String.class),
                                                 record.get(2, String.class)))
                         .filter(Objects::nonNull)
                         .collect(Collectors.toList());
    }

    @Override
    public ISchema getSchemaByName(String name) {
        String sql = dslContext.select(Tables.BITHON_META_SCHEMA.SCHEMA,
                                       Tables.BITHON_META_SCHEMA.SIGNATURE).from(Tables.BITHON_META_SCHEMA).getSQL()
                     + " FINAL where "
                     + Tables.BITHON_META_SCHEMA.NAME.eq(name);

        Record record = dslContext.fetchOne(sql);
        return record == null ? null : toSchema(name, record.get(0, String.class), record.get(1, String.class));
    }

    /**
     * clickhouse does not support update, as we're using the replacing merge tree, just to insert one record
     */
    @Override
    public void update(String name, ISchema schema) throws IOException {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        String schemaText = objectMapper.writeValueAsString(schema);
        schema.setSignature(HashUtils.sha256Hex(schemaText));

        dslContext.insertInto(Tables.BITHON_META_SCHEMA)
                  .set(Tables.BITHON_META_SCHEMA.NAME, name)
                  .set(Tables.BITHON_META_SCHEMA.SCHEMA, schemaText)
                  .set(Tables.BITHON_META_SCHEMA.SIGNATURE, schema.getSignature())
                  .set(Tables.BITHON_META_SCHEMA.TIMESTAMP, now.toLocalDateTime())
                  .execute();
    }

    @Override
    public void putIfNotExist(String name, ISchema schema) throws IOException {
        String schemaText = objectMapper.writeValueAsString(schema);
        schema.setSignature(HashUtils.sha256Hex(schemaText));

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
                  .set(Tables.BITHON_META_SCHEMA.SIGNATURE, HashUtils.sha256Hex(schemaText))
                  .set(Tables.BITHON_META_SCHEMA.TIMESTAMP, now.toLocalDateTime())
                  .execute();
    }
}
