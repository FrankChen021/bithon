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
import org.bithon.server.storage.common.expiration.ExpirationConfig;
import org.bithon.server.storage.common.expiration.IExpirationRunnable;
import org.bithon.server.storage.jdbc.JdbcStorageConfiguration;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.jdbc.common.jooq.tables.records.BithonApplicationInstanceRecord;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.meta.Instance;
import org.bithon.server.storage.meta.MetaStorageConfig;
import org.bithon.server.storage.meta.Metadata;
import org.jooq.DSLContext;
import org.jooq.InsertSetStep;
import org.jooq.InsertValuesStepN;
import org.jooq.Record2;
import org.jooq.SelectConditionStep;

import java.sql.Timestamp;
import java.util.Collection;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/11 10:56 下午
 */
@JsonTypeName("jdbc")
public class MetadataJdbcStorage implements IMetaStorage {

    protected final DSLContext dslContext;
    protected final MetaStorageConfig storageConfig;

    @JsonCreator
    public MetadataJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcStorageConfiguration storageConfigurationProvider,
                               @JacksonInject(useInput = OptBoolean.FALSE) MetaStorageConfig metaStorageConfig
    ) {
        this(storageConfigurationProvider.getDslContext(), metaStorageConfig);
    }

    public MetadataJdbcStorage(DSLContext dslContext, MetaStorageConfig metaStorageConfig) {
        this.dslContext = dslContext;
        this.storageConfig = metaStorageConfig;
    }

    @Override
    public void initialize() {
        this.dslContext.createTableIfNotExists(Tables.BITHON_APPLICATION_INSTANCE)
                       .columns(Tables.BITHON_APPLICATION_INSTANCE.fields())
                       .indexes(Tables.BITHON_APPLICATION_INSTANCE.getIndexes())
                       .execute();
    }

    @Override
    public Collection<Instance> getApplicationInstances(long since) {
        return dslContext.select(Tables.BITHON_APPLICATION_INSTANCE.APPNAME,
                                 Tables.BITHON_APPLICATION_INSTANCE.APPTYPE,
                                 Tables.BITHON_APPLICATION_INSTANCE.INSTANCENAME)
                         .from(Tables.BITHON_APPLICATION_INSTANCE)
                         .where(Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP.ge(new Timestamp(since).toLocalDateTime()))
                         .orderBy(Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP)
                         .fetchInto(Instance.class);
    }

    @Override
    public Collection<Metadata> getApplications(String appType, long since) {

        SelectConditionStep<Record2<String, String>> sql = dslContext.select(Tables.BITHON_APPLICATION_INSTANCE.APPNAME,
                                                                             Tables.BITHON_APPLICATION_INSTANCE.APPTYPE)
                                                                     .from(Tables.BITHON_APPLICATION_INSTANCE)
                                                                     .where(Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP.ge(new Timestamp(since).toLocalDateTime()));
        if (appType != null) {
            sql = sql.and(Tables.BITHON_APPLICATION_INSTANCE.APPTYPE.eq(appType));
        }

        // Use group by to de-duplicate
        return sql.groupBy(Tables.BITHON_APPLICATION_INSTANCE.APPNAME, Tables.BITHON_APPLICATION_INSTANCE.APPTYPE)
                  .orderBy(Tables.BITHON_APPLICATION_INSTANCE.APPNAME)
                  .fetchInto(Metadata.class);
    }

    @Override
    public void saveApplicationInstance(Collection<Instance> instanceList) {
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

        valueStep.onDuplicateKeyUpdate()
                 .set(Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP, new Timestamp(System.currentTimeMillis()))
                 .execute();
    }

    @Override
    public String getApplicationByInstance(String instanceName) {
        // Use ORDER-BY and LIMIT to de-duplicate
        BithonApplicationInstanceRecord instance = dslContext.selectFrom(Tables.BITHON_APPLICATION_INSTANCE)
                                                             .where(Tables.BITHON_APPLICATION_INSTANCE.INSTANCENAME.eq(instanceName))
                                                             .orderBy(Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP.desc())
                                                             .limit(1)
                                                             .fetchOne();
        return instance == null ? null : instance.getAppname();
    }

    @Override
    public boolean isApplicationExist(String applicationName) {
        // Use ORDER-BY and LIMIT to de-duplicate
        BithonApplicationInstanceRecord instance = dslContext.selectFrom(Tables.BITHON_APPLICATION_INSTANCE)
                                                             .where(Tables.BITHON_APPLICATION_INSTANCE.APPNAME.eq(applicationName))
                                                             .orderBy(Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP.desc())
                                                             .limit(1)
                                                             .fetchOne();
        return instance != null;
    }

    @Override
    public IExpirationRunnable getExpirationRunnable() {
        return new IExpirationRunnable() {
            @Override
            public ExpirationConfig getExpirationConfig() {
                return storageConfig.getTtl();
            }

            @Override
            public void expire(Timestamp before) {
                dslContext.deleteFrom(Tables.BITHON_APPLICATION_INSTANCE)
                          .where(Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP.lt(before.toLocalDateTime()))
                          .execute();
            }
        };
    }
}
