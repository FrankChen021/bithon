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
import org.bithon.server.storage.jdbc.JdbcJooqContextHolder;
import org.bithon.server.storage.jdbc.jooq.Tables;
import org.bithon.server.storage.jdbc.jooq.tables.records.BithonApplicationInstanceRecord;
import org.bithon.server.storage.meta.IMetaStorage;
import org.bithon.server.storage.meta.Metadata;
import org.bithon.server.storage.meta.MetadataType;
import org.jooq.DSLContext;
import org.springframework.dao.DuplicateKeyException;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/11 10:56 下午
 */
@JsonTypeName("jdbc")
public class MetadataJdbcStorage implements IMetaStorage {

    protected final DSLContext dslContext;

    @JsonCreator
    public MetadataJdbcStorage(@JacksonInject(useInput = OptBoolean.FALSE) JdbcJooqContextHolder dslContextHolder) {
        this(dslContextHolder.getDslContext());
    }

    public MetadataJdbcStorage(DSLContext dslContext) {
        this.dslContext = dslContext;
    }

    @Override
    public void initialize() {
        this.dslContext.createTableIfNotExists(Tables.BITHON_APPLICATION_INSTANCE)
                       .columns(Tables.BITHON_APPLICATION_INSTANCE.fields())
                       .indexes(Tables.BITHON_APPLICATION_INSTANCE.getIndexes())
                       .execute();
    }

    @Override
    public void saveApplicationInstance(String applicationName, String applicationType, String instanceName) {
        try {
            dslContext.insertInto(Tables.BITHON_APPLICATION_INSTANCE)
                      .set(Tables.BITHON_APPLICATION_INSTANCE.APPNAME, applicationName)
                      .set(Tables.BITHON_APPLICATION_INSTANCE.APPTYPE, applicationType)
                      .set(Tables.BITHON_APPLICATION_INSTANCE.INSTANCENAME, instanceName)
                      .set(Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP, new Timestamp(System.currentTimeMillis()))
                      .execute();
        } catch (DuplicateKeyException e) {
            dslContext.update(Tables.BITHON_APPLICATION_INSTANCE)
                      .set(Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP, new Timestamp(System.currentTimeMillis()))
                      .where(Tables.BITHON_APPLICATION_INSTANCE.APPNAME.eq(applicationName)
                                                                       .and(Tables.BITHON_APPLICATION_INSTANCE.APPTYPE.eq(applicationType))
                                                                       .and(Tables.BITHON_APPLICATION_INSTANCE.INSTANCENAME.eq(instanceName)))
                      .execute();
        }
    }

    @Override
    public Collection<Metadata> getMetadataByType(MetadataType type) {
        long day = 3600_000 * 24;
        return this.getApplications(System.currentTimeMillis() - day, Metadata.class);
    }

    @Override
    public String getApplicationByInstance(String instanceName) {
        BithonApplicationInstanceRecord instance = dslContext.selectFrom(Tables.BITHON_APPLICATION_INSTANCE)
                                                             .where(Tables.BITHON_APPLICATION_INSTANCE.INSTANCENAME.eq(instanceName))
                                                             .orderBy(Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP.desc())
                                                             .limit(1)
                                                             .fetchOne();
        return instance == null ? null : instance.getAppname();
    }

    @Override
    public boolean isApplicationExist(String applicationName) {
        BithonApplicationInstanceRecord instance = dslContext.selectFrom(Tables.BITHON_APPLICATION_INSTANCE)
                                                             .where(Tables.BITHON_APPLICATION_INSTANCE.APPNAME.eq(applicationName))
                                                             .orderBy(Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP.desc())
                                                             .limit(1)
                                                             .fetchOne();
        return instance != null;
    }

    private <T> List<T> getApplications(long since, Class<T> clazz) {
        return dslContext.select(Tables.BITHON_APPLICATION_INSTANCE.APPNAME, Tables.BITHON_APPLICATION_INSTANCE.APPTYPE)
                         .from(Tables.BITHON_APPLICATION_INSTANCE)
                         .where(Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP.ge(new Timestamp(since)))
                         .groupBy(Tables.BITHON_APPLICATION_INSTANCE.APPNAME, Tables.BITHON_APPLICATION_INSTANCE.APPTYPE)
                         .orderBy(Tables.BITHON_APPLICATION_INSTANCE.APPNAME)
                         .fetchInto(clazz);
    }
}
