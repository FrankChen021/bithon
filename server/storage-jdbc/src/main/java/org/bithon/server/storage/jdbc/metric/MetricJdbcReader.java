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

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.query.IDataSourceReader;
import org.bithon.server.storage.datasource.query.OrderBy;
import org.bithon.server.storage.datasource.query.Query;
import org.bithon.server.storage.datasource.query.ast.Column;
import org.bithon.server.storage.datasource.query.ast.SelectExpression;
import org.bithon.server.storage.datasource.query.ast.StringNode;
import org.bithon.server.storage.jdbc.common.dialect.Expression2Sql;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:39 下午
 */
@Slf4j
public class MetricJdbcReader implements IDataSourceReader {
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    private static final String TIMESTAMP_ALIAS_NAME = "_timestamp";

    protected final DSLContext dslContext;
    protected final ISqlDialect sqlDialect;
    private final boolean shouldCloseContext;

    public MetricJdbcReader(String name,
                            Map<String, Object> props,
                            ISqlDialect sqlDialect) {
        DruidDataSource jdbcDataSource = new DruidDataSource();
        jdbcDataSource.setDriverClassName((String) Preconditions.checkNotNull(props.get("driverClassName"), "Missing driverClassName property for %s", name));
        jdbcDataSource.setUrl((String) Preconditions.checkNotNull(props.get("url"), "Missing url property for %s", name));
        jdbcDataSource.setUsername((String) Preconditions.checkNotNull(props.get("username"), "Missing userName property for %s", name));
        jdbcDataSource.setPassword((String) Preconditions.checkNotNull(props.get("password"), "Missing password property for %s", name));
        // Make sure the name is unique to avoid exception thrown when closing the data source
        jdbcDataSource.setName(name + "-" + SEQUENCE.getAndIncrement());
        jdbcDataSource.setTestWhileIdle(false);
        jdbcDataSource.setAsyncInit(false);
        jdbcDataSource.setMaxWait(5_000);
        jdbcDataSource.setMaxCreateTaskCount(2);

        // Create a new one
        JooqAutoConfiguration autoConfiguration = new JooqAutoConfiguration();
        this.dslContext = DSL.using(new DefaultConfiguration()
                                        .set(new DataSourceConnectionProvider(jdbcDataSource))
                                        .set(new JooqProperties().determineSqlDialect(jdbcDataSource))
                                        .set(autoConfiguration.jooqExceptionTranslatorExecuteListenerProvider()));

        this.sqlDialect = sqlDialect;
        this.shouldCloseContext = true;
    }

    public MetricJdbcReader(DSLContext dslContext, ISqlDialect sqlDialect) {
        this.dslContext = dslContext;
        this.sqlDialect = sqlDialect;
        this.shouldCloseContext = false;
    }

    @Override
    public List<Map<String, Object>> timeseries(Query query) {
        SelectExpression selectExpression = SelectExpressionBuilder.builder()
                                                                   .dataSource(query.getSchema())
                                                                   .fields(query.getResultColumns())
                                                                   .filter(query.getFilter())
                                                                   .interval(query.getInterval())
                                                                   .groupBys(query.getGroupBy())
                                                                   .orderBy(OrderBy.builder().name(TIMESTAMP_ALIAS_NAME).build())
                                                                   .sqlDialect(this.sqlDialect)
                                                                   .build();

        SelectExpression timestampFilterExpression = selectExpression;
        if (selectExpression.getFrom().getExpression() instanceof SelectExpression) {
            // Has a sub-query, timestampExpression will be put in sub-query
            timestampFilterExpression = (SelectExpression) selectExpression.getFrom().getExpression();

            // Add timestamp field to an outer query at first position
            selectExpression.getResultColumnList().insert(new Column(TIMESTAMP_ALIAS_NAME));
        }

        // Add timestamp expression to sub-query
        timestampFilterExpression.getResultColumnList()
                                 .insert(new StringNode(StringUtils.format("%s AS \"%s\"",
                                                                           sqlDialect.timeFloorExpression(query.getInterval().getTimestampColumn(),
                                                                                                          query.getInterval().getStep()),
                                                                           TIMESTAMP_ALIAS_NAME)));

        selectExpression.getGroupBy().addField(TIMESTAMP_ALIAS_NAME);

        SqlGenerator sqlGenerator = new SqlGenerator(this.sqlDialect);
        selectExpression.accept(sqlGenerator);
        return executeSql(sqlGenerator.getSQL());
    }

    @Override
    public List<?> groupBy(Query query) {
        SelectExpression selectExpression = SelectExpressionBuilder.builder()
                                                                   .dataSource(query.getSchema())
                                                                   .fields(query.getResultColumns())
                                                                   .filter(query.getFilter())
                                                                   .interval(query.getInterval())
                                                                   .groupBys(query.getGroupBy())
                                                                   .orderBy(query.getOrderBy())
                                                                   .limit(query.getLimit())
                                                                   .sqlDialect(this.sqlDialect)
                                                                   .build();

        SqlGenerator sqlGenerator = new SqlGenerator(this.sqlDialect);
        selectExpression.accept(sqlGenerator);
        return fetch(sqlGenerator.getSQL(), query.getResultFormat());
    }

    private String getOrderBySQL(OrderBy orderBy, String timestampColumn) {
        if (orderBy == null) {
            return "";
        }

        return "ORDER BY \"" + orderBy.getName() + "\" " + orderBy.getOrder();
    }

    @Override
    public List<Map<String, Object>> select(Query query) {
        String sqlTableName = query.getSchema().getDataStoreSpec().getStore();
        String timestampCol = query.getSchema().getTimestampSpec().getColumnName();
        String filter = Expression2Sql.from(query.getSchema(), sqlDialect, query.getFilter());
        String sql = StringUtils.format(
            "SELECT %s FROM \"%s\" WHERE %s %s \"%s\" >= %s AND \"%s\" < %s %s LIMIT %d OFFSET %d",
            query.getResultColumns()
                 .stream()
                 .map(field -> {
                     String expr = field.getColumnExpression().toString();
                     String alias = field.getResultColumnName();

                     return expr.equals(alias) ?
                         StringUtils.format("\"%s\"", field.getColumnExpression())
                         :
                         StringUtils.format("\"%s\" AS \"%s\"", field.getColumnExpression(), field.getResultColumnName());
                 })
                 .collect(Collectors.joining(",")),
            sqlTableName,
            StringUtils.hasText(filter) ? filter : "",
            StringUtils.hasText(filter) ? "AND" : "",
            timestampCol,
            sqlDialect.formatTimestamp(query.getInterval().getStartTime()),
            timestampCol,
            sqlDialect.formatTimestamp(query.getInterval().getEndTime()),
            getOrderBySQL(query.getOrderBy(), timestampCol),
            query.getLimit().getLimit(),
            query.getLimit().getOffset());

        return executeSql(sql);
    }

    @Override
    public int count(Query query) {
        String sqlTableName = query.getSchema().getDataStoreSpec().getStore();
        String timestampCol = query.getSchema().getTimestampSpec().getColumnName();

        String filter = Expression2Sql.from(query.getSchema(), sqlDialect, query.getFilter());
        String sql = StringUtils.format(
            "SELECT count(1) FROM \"%s\" WHERE %s %s \"%s\" >= %s AND \"%s\" < %s",
            sqlTableName,
            StringUtils.hasText(filter) ? filter : "",
            StringUtils.hasText(filter) ? "AND" : "",
            timestampCol,
            sqlDialect.formatTimestamp(query.getInterval().getStartTime()),
            timestampCol,
            sqlDialect.formatTimestamp(query.getInterval().getEndTime())
        );

        Record record = dslContext.fetchOne(sql);
        return ((Number) record.get(0)).intValue();
    }

    private List<?> fetch(String sql, Query.ResultFormat resultFormat) {
        log.info("Executing {}", sql);

        List<Record> records = dslContext.fetch(sql);

        if (resultFormat == Query.ResultFormat.Object) {
            return records.stream().map(record -> {
                Map<String, Object> mapObject = new HashMap<>(record.fields().length);
                for (Field<?> field : record.fields()) {
                    mapObject.put(field.getName(), record.get(field));
                }
                return mapObject;
            }).collect(Collectors.toList());
        } else {
            return records.stream().map(Record::intoArray).collect(Collectors.toList());
        }
    }

    private List<Map<String, Object>> executeSql(String sql) {
        log.info("Executing {}", sql);

        List<Record> records = dslContext.fetch(sql);

        // PAY ATTENTION:
        //  although the explicit cast seems unnecessary, it must be kept so that compilation can pass
        //  this is might be a bug of JDK
        return (List<Map<String, Object>>) records.stream().map(record -> {
            Map<String, Object> mapObject = new HashMap<>(record.fields().length);
            for (Field<?> field : record.fields()) {
                mapObject.put(field.getName(), record.get(field));
            }
            return mapObject;
        }).collect(Collectors.toList());
    }

    @Override
    public List<String> distinct(Query query) {
        String filterText = query.getFilter() == null ? "" : Expression2Sql.from(query.getSchema(), sqlDialect, query.getFilter()) + " AND ";
        String dimension = query.getResultColumns().get(0).getResultColumnName();

        String sql = StringUtils.format(
            "SELECT DISTINCT(\"%s\") \"%s\" FROM \"%s\" WHERE %s \"timestamp\" >= %s AND \"timestamp\" < %s AND \"%s\" IS NOT NULL ORDER BY \"%s\"",
            dimension,
            dimension,
            query.getSchema().getDataStoreSpec().getStore(),
            filterText,
            sqlDialect.formatTimestamp(query.getInterval().getStartTime()),
            sqlDialect.formatTimestamp(query.getInterval().getEndTime()),
            dimension,
            dimension
        );

        log.info("Executing {}", sql);
        List<Record> records = dslContext.fetch(sql);
        return records.stream()
                      .map(record -> record.get(0).toString())
                      .collect(Collectors.toList());
    }

    @Override
    public void close() {
        if (this.shouldCloseContext) {
            try {
                DataSourceConnectionProvider cp = (DataSourceConnectionProvider) this.dslContext.configuration().connectionProvider();
                ((DruidDataSource) cp.dataSource()).close();
            } catch (Exception ignored) {
            }
            this.dslContext.close();
        }
    }
}
