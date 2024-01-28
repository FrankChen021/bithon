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

package org.bithon.server.storage.jdbc.metric;

import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.common.expiration.ExpirationConfig;
import org.bithon.server.storage.datasource.DataSourceSchemaManager;
import org.bithon.server.storage.datasource.IDataSource;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.bithon.server.storage.metrics.ttl.MetricStorageCleaner;
import org.jooq.DSLContext;
import org.jooq.DeleteConditionStep;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 5/1/24 10:34 am
 */
public class MetricJdbcStorageCleaner extends MetricStorageCleaner {
    protected final DSLContext dslContext;
    protected final DataSourceSchemaManager schemaManager;
    protected final ExpirationConfig ttlConfig;
    protected final ISqlDialect sqlDialect;

    protected MetricJdbcStorageCleaner(DSLContext dslContext,
                                       DataSourceSchemaManager schemaManager,
                                       ExpirationConfig ttlConfig,
                                       ISqlDialect sqlDialect) {
        this.dslContext = dslContext;
        this.schemaManager = schemaManager;
        this.ttlConfig = ttlConfig;
        this.sqlDialect = sqlDialect;
    }

    @Override
    public ExpirationConfig getExpirationConfig() {
        return ttlConfig;
    }

    @Override
    protected DataSourceSchemaManager getSchemaManager() {
        return schemaManager;
    }

    @Override
    protected final List<TimeSpan> getSkipDateList() {
        return getSkipDateRecordList().stream()
                                      .map((record) -> {
                                          try {
                                              String date = record.get(Tables.BITHON_METRICS_BASELINE.DATE);
                                              TimeSpan startTimestamp = TimeSpan.of(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH).parse(date + " 00:00:00").getTime());

                                              int keepDays = record.get(Tables.BITHON_METRICS_BASELINE.KEEP_DAYS);
                                              if (keepDays > 0) {
                                                  if (startTimestamp.after(keepDays, TimeUnit.DAYS).getMilliseconds() > System.currentTimeMillis()) {
                                                      return startTimestamp;
                                                  } else {
                                                      // Will be ignored
                                                      return null;
                                                  }
                                              } else {
                                                  return startTimestamp;
                                              }
                                          } catch (ParseException e) {
                                              return null;
                                          }
                                      }).filter(Objects::nonNull)
                                      .collect(Collectors.toList());
    }

    protected Result<? extends Record> getSkipDateRecordList() {
        return dslContext.selectFrom(Tables.BITHON_METRICS_BASELINE)
                         .fetch();
    }

    @SuppressWarnings("rawtypes")
    static class DeleteTable extends TableImpl {
        final Field<Timestamp> timestampField;

        public DeleteTable(String name) {
            super(DSL.name(name));

            //noinspection unchecked
            timestampField = createField(DSL.name("timestamp"), SQLDataType.TIMESTAMP);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected void expireImpl(IDataSource schema, Timestamp before, List<TimeSpan> skipDateList) {
        final DeleteTable table = new DeleteTable(schema.getDataStoreSpec().getStore());
        DeleteConditionStep delete = dslContext.deleteFrom(table)
                                               .where(table.timestampField.le(before));
        if (!skipDateList.isEmpty()) {
            String skipSql = skipDateList.stream()
                                         .map((skipDate) -> {
                                             TimeSpan endTimestamp = skipDate.after(1, TimeUnit.DAYS);
                                             String timestampField = sqlDialect.quoteIdentifier(table.timestampField.getName());
                                             return StringUtils.format("NOT (%s >= '%s' AND %s < '%s')", timestampField, skipDate.toISO8601(), timestampField, endTimestamp.toISO8601());
                                         })
                                         .filter((s) -> !s.isEmpty())
                                         .collect(Collectors.joining(" AND "));
            if (!skipSql.isEmpty()) {
                delete = delete.and(skipSql);
            }
        }

        delete.execute();
    }
}
