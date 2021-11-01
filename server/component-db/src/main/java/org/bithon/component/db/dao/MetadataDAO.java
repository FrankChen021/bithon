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

package org.bithon.component.db.dao;

import org.bithon.component.db.jooq.DefaultSchema;
import org.bithon.component.db.jooq.Tables;
import org.bithon.component.db.jooq.tables.records.BithonApplicationInstanceRecord;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.springframework.dao.DuplicateKeyException;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/11 10:40 下午
 */
public class MetadataDAO {
    private final DSLContext dsl;

    public DSLContext getDsl() {
        return dsl;
    }

    public MetadataDAO(DSLContext dsl) {
        this.dsl = dsl;
        if (dsl.configuration().dialect().equals(SQLDialect.H2)) {
            this.dsl.createTableIfNotExists(Tables.BITHON_APPLICATION_INSTANCE)
                    .columns(Tables.BITHON_APPLICATION_INSTANCE.APPLICATION_NAME,
                             Tables.BITHON_APPLICATION_INSTANCE.APPLICATION_TYPE,
                             Tables.BITHON_APPLICATION_INSTANCE.INSTANCE_NAME,
                             Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP)
                    .indexes(DefaultSchema.DEFAULT_SCHEMA.BITHON_APPLICATION_INSTANCE.getIndexes())
                    .execute();
        }
    }

    public void saveApplicationInstance(String applicationName, String applicationType, String instanceName) {
        try {
            dsl.insertInto(Tables.BITHON_APPLICATION_INSTANCE)
               .set(Tables.BITHON_APPLICATION_INSTANCE.APPLICATION_NAME, applicationName)
               .set(Tables.BITHON_APPLICATION_INSTANCE.APPLICATION_TYPE, applicationType)
               .set(Tables.BITHON_APPLICATION_INSTANCE.INSTANCE_NAME, instanceName)
               .set(Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP, new Timestamp(System.currentTimeMillis()))
               .execute();
        } catch (DuplicateKeyException e) {
            dsl.update(Tables.BITHON_APPLICATION_INSTANCE)
               .set(Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP, new Timestamp(System.currentTimeMillis()))
               .where(Tables.BITHON_APPLICATION_INSTANCE.APPLICATION_NAME.eq(applicationName)
                                                                         .and(Tables.BITHON_APPLICATION_INSTANCE.APPLICATION_TYPE.eq(
                                                                             applicationType))
                                                                         .and(Tables.BITHON_APPLICATION_INSTANCE.INSTANCE_NAME.eq(
                                                                             instanceName)))
               .execute();
        }
    }

    public BithonApplicationInstanceRecord getByApplicationName(String applicationName) {
        return dsl.selectFrom(Tables.BITHON_APPLICATION_INSTANCE)
                  .where(Tables.BITHON_APPLICATION_INSTANCE.APPLICATION_NAME.eq(applicationName))
                  .orderBy(Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP.desc())
                  .limit(1)
                  .fetchOne();
    }

    public BithonApplicationInstanceRecord getByInstanceName(String instanceName) {
        return dsl.selectFrom(Tables.BITHON_APPLICATION_INSTANCE)
                  .where(Tables.BITHON_APPLICATION_INSTANCE.INSTANCE_NAME.eq(instanceName))
                  .orderBy(Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP.desc())
                  .limit(1)
                  .fetchOne();
    }

    public <T> List<T> getApplications(long since, Class<T> clazz) {
        return dsl.select(Tables.BITHON_APPLICATION_INSTANCE.APPLICATION_NAME, Tables.BITHON_APPLICATION_INSTANCE.APPLICATION_TYPE)
                  .from(Tables.BITHON_APPLICATION_INSTANCE)
                  .where(Tables.BITHON_APPLICATION_INSTANCE.TIMESTAMP.ge(new Timestamp(since)))
                  .groupBy(Tables.BITHON_APPLICATION_INSTANCE.APPLICATION_NAME, Tables.BITHON_APPLICATION_INSTANCE.APPLICATION_TYPE)
                  .orderBy(Tables.BITHON_APPLICATION_INSTANCE.APPLICATION_NAME)
                  .fetchInto(clazz);
    }
}
