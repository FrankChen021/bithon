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
import org.bithon.server.storage.common.IExpirationRunnable;
import org.bithon.server.storage.common.ExpirationConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseJooqContextHolder;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.jdbc.meta.MetadataJdbcStorage;
import org.bithon.server.storage.meta.Instance;
import org.bithon.server.storage.meta.MetaStorageConfig;
import org.jooq.InsertSetStep;
import org.jooq.InsertValuesStepN;
import org.jooq.Record;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/10/27 9:56 下午
 */
@JsonTypeName("clickhouse")
public class MetadataStorage extends MetadataJdbcStorage {

    private final ClickHouseConfig config;

    @JsonCreator
    public MetadataStorage(@JacksonInject(useInput = OptBoolean.FALSE) ClickHouseJooqContextHolder dslContextHolder,
                           @JacksonInject(useInput = OptBoolean.FALSE) MetaStorageConfig storageConfig,
                           @JacksonInject(useInput = OptBoolean.FALSE) ClickHouseConfig config) {
        super(dslContextHolder.getDslContext(), storageConfig);
        this.config = config;
    }

    @Override
    public void initialize() {
        new TableCreator(config, this.dslContext).useReplacingMergeTree(Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP.getName())
                                                 // No partition for this table
                                                 .partitionByExpression(null)
                                                 // Add minmax index to timestamp column
                                                 .secondaryIndex(Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP.getName(), new TableCreator.SecondaryIndex("minmax", 4096))
                                                 .createIfNotExist(Tables.BITHON_APPLICATION_INSTANCE);
    }

    @Override
    public Collection<Instance> getApplicationInstances(long since) {
        String sql = dslContext.select(Tables.BITHON_APPLICATION_INSTANCE.APPNAME,
                                       Tables.BITHON_APPLICATION_INSTANCE.APPTYPE,
                                       Tables.BITHON_APPLICATION_INSTANCE.INSTANCENAME)
                               .from(Tables.BITHON_APPLICATION_INSTANCE)
                               .getSQL() + " FINAL WHERE ";

        sql += dslContext.renderInlined(Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP.ge(new Timestamp(since).toLocalDateTime()));
        sql += " ORDER BY " + Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP.getName();

        List<Record> records = dslContext.fetch(sql);
        if (records == null) {
            return Collections.emptyList();
        }

        return records.stream().map((mapper) -> {
            String name = mapper.get(0, String.class);
            String type = mapper.get(1, String.class);
            String instance = mapper.get(2, String.class);
            return new Instance(name, type, instance);
        }).collect(Collectors.toSet());
    }

    @Override
    public void saveApplicationInstance(List<Instance> instanceList) {
        InsertSetStep step = dslContext.insertInto(Tables.BITHON_APPLICATION_INSTANCE);

        InsertValuesStepN valueStep = null;
        for (Instance inputRow : instanceList) {
            Object[] values = new Object[4];
            values[0] = new Timestamp(System.currentTimeMillis()).toLocalDateTime();
            values[1] = inputRow.getAppName();
            values[2] = inputRow.getAppType();
            values[3] = inputRow.getInstanceName();
            if (valueStep == null) {
                valueStep = step.values(values);
            } else {
                valueStep = valueStep.values(values);
            }
        }

        // No need to ignore or update because we use ReplacingMergeTree for this table
        // The duplication will be handled by the underlying storage
        valueStep.execute();
    }

    @Override
    public IExpirationRunnable getExpirationRunnable() {
        return new IExpirationRunnable() {
            @Override
            public ExpirationConfig getRule() {
                return storageConfig.getTtl();
            }

            @Override
            public void expire(Timestamp before) {
                new DataCleaner(config, dslContext).deleteFromTable(Tables.BITHON_APPLICATION_INSTANCE, before);
            }
        };
    }
}
