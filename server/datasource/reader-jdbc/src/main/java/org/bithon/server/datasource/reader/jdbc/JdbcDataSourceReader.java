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

package org.bithon.server.datasource.reader.jdbc;

import com.alibaba.druid.pool.DruidDataSource;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.datasource.query.IDataSourceReader;
import org.bithon.server.datasource.query.Interval;
import org.bithon.server.datasource.query.Limit;
import org.bithon.server.datasource.query.Order;
import org.bithon.server.datasource.query.OrderBy;
import org.bithon.server.datasource.query.Query;
import org.bithon.server.datasource.query.ast.Selector;
import org.bithon.server.datasource.query.pipeline.ColumnarTable;
import org.bithon.server.datasource.query.pipeline.IQueryStep;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.pipeline.JdbcPipelineBuilder;
import org.bithon.server.datasource.reader.jdbc.statement.SqlGenerator;
import org.bithon.server.datasource.reader.jdbc.statement.ast.LimitClause;
import org.bithon.server.datasource.reader.jdbc.statement.ast.OrderByClause;
import org.bithon.server.datasource.reader.jdbc.statement.ast.SelectStatement;
import org.bithon.server.datasource.reader.jdbc.statement.ast.TableIdentifier;
import org.bithon.server.datasource.reader.jdbc.statement.ast.TextNode;
import org.bithon.server.datasource.reader.jdbc.statement.builder.SelectStatementBuilder;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.springframework.boot.autoconfigure.jooq.ExceptionTranslatorExecuteListener;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqProperties;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/31 1:39 下午
 */
@Slf4j
public class JdbcDataSourceReader implements IDataSourceReader {
    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    protected final DSLContext dslContext;
    protected final ISqlDialect sqlDialect;
    private final boolean shouldCloseContext;

    public JdbcDataSourceReader(String name,
                                Map<String, Object> props,
                                ISqlDialect sqlDialect) {
        DruidDataSource jdbcDataSource = new DruidDataSource();
        jdbcDataSource.setDriverClassName((String) Preconditions.checkNotNull(props.get("driverClassName"), "Missing driverClassName property for %s", name));
        jdbcDataSource.setUrl((String) Preconditions.checkNotNull(props.get("url"), "Missing url property for %s", name));
        jdbcDataSource.setUsername((String) Preconditions.checkNotNull(props.get("username"), "Missing userName property for %s", name));

        String password = (String) props.get("password");
        if (!StringUtils.isEmpty(password)) {
            // For local or test environment database deployment,
            // password is not mandatory
            jdbcDataSource.setPassword(password);
        }

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
                                        .set(autoConfiguration.jooqExceptionTranslatorExecuteListenerProvider(ExceptionTranslatorExecuteListener.DEFAULT)));

        this.sqlDialect = sqlDialect;
        this.shouldCloseContext = true;
    }

    public JdbcDataSourceReader(DSLContext dslContext, ISqlDialect sqlDialect) {
        this.dslContext = dslContext;
        this.sqlDialect = sqlDialect;
        this.shouldCloseContext = false;
    }

    @Override
    public ColumnarTable timeseries(Query query) {
        SelectStatementBuilder statementBuilder = SelectStatementBuilder.builder()
                                                                        .schema(query.getSchema())
                                                                        .fields(query.getSelectors())
                                                                        .filter(query.getFilter())
                                                                        .interval(query.getInterval())
                                                                        .groupBy(query.getGroupBy())
                                                                        .orderBy(query.getOrderBy())
                                                                        .offset(query.getOffset())
                                                                        .sqlDialect(this.sqlDialect);

        SelectStatement selectStatement = statementBuilder.build();

        Interval interval = query.getInterval();

        IQueryStep queryStep = JdbcPipelineBuilder.builder()
                                                  .dslContext(dslContext)
                                                  .dialect(this.sqlDialect)
                                                  .selectStatement(selectStatement)
                                                  .interval(Interval.of(interval.getStartTime().floor(query.getInterval().getStep()),
                                                                        interval.getEndTime(),
                                                                        interval.getStep(),
                                                                        null,
                                                                        null))
                                                  .build();

        try {
            return queryStep.execute()
                            .get()
                            .getTable();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<?> groupBy(Query query) {
        SelectStatement selectStatement = SelectStatementBuilder.builder()
                                                                .schema(query.getSchema())
                                                                .fields(query.getSelectors())
                                                                .filter(query.getFilter())
                                                                .interval(query.getInterval())
                                                                .groupBy(query.getGroupBy())
                                                                .orderBy(query.getOrderBy())
                                                                .limit(query.getLimit())
                                                                .offset(query.getOffset())
                                                                .sqlDialect(this.sqlDialect)
                                                                .build();

        SqlGenerator sqlGenerator = new SqlGenerator(this.sqlDialect);
        sqlGenerator.generate(selectStatement);
        return fetch(sqlGenerator.getSQL(), query.getResultFormat());
    }

    @Override
    public List<?> select(Query query) {
        IdentifierExpression timestampCol = IdentifierExpression.of(query.getSchema().getTimestampSpec().getColumnName());

        SelectStatement selectStatement = new SelectStatement();
        selectStatement.getFrom().setExpression(new TableIdentifier(query.getSchema().getDataStoreSpec().getStore()));
        for (Selector selector : query.getSelectors()) {
            selectStatement.getSelectorList().add(selector.getSelectExpression(), selector.getOutput(), selector.getDataType());
        }
        selectStatement.getWhere().and(new ComparisonExpression.GTE(timestampCol, sqlDialect.toTimestampExpression(query.getInterval().getStartTime())));
        selectStatement.getWhere().and(new ComparisonExpression.LT(timestampCol, sqlDialect.toTimestampExpression(query.getInterval().getEndTime())));
        selectStatement.getWhere().and(sqlDialect.transform(query.getSchema(), query.getFilter()));
        selectStatement.setLimit(toLimitClause(query.getLimit()));
        selectStatement.setOrderBy(toOrderByClause(query.getOrderBy()));
        SqlGenerator generator = new SqlGenerator(sqlDialect);
        generator.generate(selectStatement);
        String sql = generator.getSQL();

        return executeSql(sql);
    }

    @Override
    public int count(Query query) {
        IdentifierExpression timestampCol = IdentifierExpression.of(query.getSchema().getTimestampSpec().getColumnName());

        SelectStatement selectStatement = new SelectStatement();
        selectStatement.getFrom().setExpression(new TableIdentifier(query.getSchema().getDataStoreSpec().getStore()));
        selectStatement.getSelectorList().add(new TextNode("count(1)"), IDataType.LONG);
        selectStatement.getWhere().and(new ComparisonExpression.GTE(timestampCol, sqlDialect.toTimestampExpression(query.getInterval().getStartTime())));
        selectStatement.getWhere().and(new ComparisonExpression.LT(timestampCol, sqlDialect.toTimestampExpression(query.getInterval().getEndTime())));
        selectStatement.getWhere().and(sqlDialect.transform(query.getSchema(), query.getFilter()));
        SqlGenerator generator = new SqlGenerator(sqlDialect);
        generator.generate(selectStatement);
        String sql = generator.getSQL();

        log.info("Executing {}", sql);
        Record record = dslContext.fetchOne(sql);
        return ((Number) record.get(0)).intValue();
    }

    private List<?> fetch(String sql, Query.ResultFormat resultFormat) {
        log.info("Executing {}", sql);

        List<Record> records = dslContext.fetch(sql);

        if (resultFormat == Query.ResultFormat.Object) {
            return records.stream().map(record -> {
                Map<String, Object> mapObject = new LinkedHashMap<>(record.fields().length);
                for (Field<?> field : record.fields()) {
                    mapObject.put(field.getName(), record.get(field));
                }
                return mapObject;
            }).collect(Collectors.toList());
        } else {
            return records.stream().map(Record::intoArray).collect(Collectors.toList());
        }
    }

    @Override
    public List<String> distinct(Query query) {
        IdentifierExpression timestampCol = IdentifierExpression.of(query.getSchema().getTimestampSpec().getColumnName());

        String dimension = query.getSelectors().get(0).getOutputName();

        SelectStatement selectStatement = new SelectStatement();
        selectStatement.getFrom().setExpression(new TableIdentifier(query.getSchema().getDataStoreSpec().getStore()));
        selectStatement.getSelectorList().add(new TextNode(StringUtils.format("DISTINCT(%s)", sqlDialect.quoteIdentifier(dimension))), dimension, IDataType.STRING);
        selectStatement.getWhere().and(new ComparisonExpression.GTE(timestampCol, sqlDialect.toTimestampExpression(query.getInterval().getStartTime())));
        selectStatement.getWhere().and(new ComparisonExpression.LT(timestampCol, sqlDialect.toTimestampExpression(query.getInterval().getEndTime())));
        selectStatement.getWhere().and(sqlDialect.transform(query.getSchema(), query.getFilter()));
        selectStatement.getWhere().and(new ComparisonExpression.NE(IdentifierExpression.of(dimension), new LiteralExpression.StringLiteral("")));
        selectStatement.setOrderBy(new OrderByClause(dimension, Order.asc));
        SqlGenerator generator = new SqlGenerator(sqlDialect);
        generator.generate(selectStatement);
        String sql = generator.getSQL();

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
        }
    }

    private OrderByClause toOrderByClause(OrderBy orderBy) {
        return orderBy == null ? null : new OrderByClause(orderBy.getName(), orderBy.getOrder());
    }

    public LimitClause toLimitClause(Limit limit) {
        return limit == null ? null : new LimitClause(limit.getLimit(), limit.getOffset());
    }

    private List<Map<String, Object>> executeSql(String sql) {
        log.info("Executing {}", sql);

        List<Record> records = dslContext.fetch(sql);

        // PAY ATTENTION:
        //  although the explicit cast seems unnecessary, it must be kept so that compilation can pass
        //  this might be a bug of JDK
        return (List<Map<String, Object>>) records.stream().map(record -> {
            Map<String, Object> mapObject = new HashMap<>(record.fields().length);
            for (Field<?> field : record.fields()) {
                mapObject.put(field.getName(), record.get(field));
            }
            return mapObject;
        }).collect(Collectors.toList());
    }
}
