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

package org.bithon.server.storage.jdbc.clickhouse.metric;

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.query.Query;
import org.bithon.server.storage.jdbc.common.dialect.Expression2Sql;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;
import org.bithon.server.storage.jdbc.metric.MetricJdbcReader;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 29/1/24 11:26 am
 */
@Slf4j
public class JdbcReader extends MetricJdbcReader {

    public JdbcReader(String name,
                      Map<String, Object> props,
                      ISqlDialect sqlDialect) {
        super(name, props, sqlDialect);
    }

    public JdbcReader(DSLContext dslContext, ISqlDialect sqlDialect) {
        super(dslContext, sqlDialect);
    }

    /**
     * Rewrite the SQL to use group-by instead of distinct so that we can leverage PROJECTIONS defined at the underlying table to speed up queries
     */
    @Override
    public List<String> distinct(Query query) {
        TimeSpan start = query.getInterval().getStartTime().floor(Duration.ofMinutes(1));
        TimeSpan end = query.getInterval().getEndTime().ceil(Duration.ofMinutes(1));

        String dimension = query.getSelectors().get(0).getOutputName();
        String condition = query.getFilter() == null ? "" : Expression2Sql.from(query.getSchema(), sqlDialect, query.getFilter()) + " AND ";

        String sql = StringUtils.format(
            "SELECT \"%s\" FROM \"%s\" WHERE %s toStartOfMinute(\"timestamp\") >= %s AND toStartOfMinute(\"timestamp\") < %s GROUP BY \"%s\" ORDER BY \"%s\"",
            dimension,
            query.getSchema().getDataStoreSpec().getStore(),
            condition,
            sqlDialect.toTimestampExpression(start).serializeToText(),
            sqlDialect.toTimestampExpression(end).serializeToText(),
            dimension,
            dimension);

        log.info("Executing {}", sql);
        List<Record> records = dslContext.fetch(sql);
        return records.stream()
                      .map(record -> record.get(0).toString())
                      .collect(Collectors.toList());
    }
}
