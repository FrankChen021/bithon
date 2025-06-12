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

package org.bithon.server.datasource.reader.jdbc.pipeline;


import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.TimestampSpec;
import org.bithon.server.datasource.query.Interval;
import org.bithon.server.datasource.query.ast.ExpressionNode;
import org.bithon.server.datasource.query.ast.Selector;
import org.bithon.server.datasource.query.plan.logical.LogicalAggregate;
import org.bithon.server.datasource.query.plan.physical.IPhysicalPlan;
import org.bithon.server.datasource.query.plan.physical.PhysicalPlanSerializer;
import org.bithon.server.datasource.query.result.Column;
import org.bithon.server.datasource.query.result.ColumnarTable;
import org.bithon.server.datasource.query.result.PipelineQueryResult;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.statement.ast.SelectStatement;
import org.bithon.server.datasource.reader.jdbc.statement.builder.SelectStatementBuilder;
import org.jooq.Cursor;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author frank.chen021@outlook.com
 * @date 5/5/25 6:14 pm
 */
@Slf4j
public class JdbcReadStep implements IPhysicalPlan {

    private final DSLContext dslContext;
    private final String sql;

    @Getter
    private final SelectStatement selectStatement;
    private final ISqlDialect sqlDialect;
    private final ISchema schema;
    private final Interval interval;
    private final List<String> keyColumns;
    private final List<String> valColumns;
    private final HumanReadableDuration offset;

    public JdbcReadStep(DSLContext dslContext,
                        ISchema schema,
                        ISqlDialect sqlDialect,
                        SelectStatement selectStatement,
                        Interval interval,
                        List<String> keyColumns,
                        List<String> valueColumns
    ) {
        this.dslContext = dslContext;
        this.schema = schema;
        this.selectStatement = selectStatement;
        this.interval = interval;
        this.sqlDialect = sqlDialect;
        this.sql = selectStatement.toSQL(sqlDialect);
        this.keyColumns = keyColumns;
        this.valColumns = valueColumns;
        this.offset = null;
    }

    public JdbcReadStep(DSLContext dslContext,
                        ISchema schema,
                        ISqlDialect sqlDialect,
                        SelectStatement selectStatement,
                        Interval interval,
                        List<String> keyColumns,
                        List<String> valueColumns,
                        HumanReadableDuration offset
    ) {
        this.dslContext = dslContext;
        this.schema = schema;
        this.selectStatement = selectStatement;
        this.interval = interval;
        this.sqlDialect = sqlDialect;
        this.sql = selectStatement.toSQL(sqlDialect);
        this.keyColumns = keyColumns;
        this.valColumns = valueColumns;
        this.offset = offset;
    }

    @Override
    public IPhysicalPlan offset(HumanReadableDuration offset) {
        return new JdbcReadStep(
            this.dslContext,
            this.schema,
            this.sqlDialect,
            this.selectStatement,
            this.interval,
            this.keyColumns,
            this.valColumns,
            this.offset
        );
    }

    @Override
    public void serializer(PhysicalPlanSerializer serializer) {
        String stepName = this.getClass().getSimpleName();
        serializer.append(stepName).append('\n');
        serializer.append("    ", this.sql);
    }

    @Override
    public String toString() {
        return this.sql;
    }

    @Override
    public boolean isScalar() {
        return false;
    }

    @Override
    public boolean canPushDownAggregate(LogicalAggregate aggregate) {
        return true;
    }

    @Override
    public IPhysicalPlan pushDownAggregate(LogicalAggregate aggregate) {
        SelectStatementBuilder builder = new SelectStatementBuilder();
        SelectStatement select = builder.sqlDialect(this.sqlDialect)
                                        .schema(this.schema)
                                        .interval(this.interval)
                                        .fields(List.of(new Selector(new ExpressionNode(this.schema,
                                                                                        StringUtils.format("%s(%s)", aggregate.func(), aggregate.field().getName())),
                                                                     aggregate.field().getName(),
                                                                     aggregate.field().getDataType())))
                                        .filter(LogicalExpression.create(LogicalExpression.AND.OP, selectStatement.getWhere().getExpressions()))
                                        .groupBy(aggregate.groupBy())
                                        .build();

        List<String> keys = new ArrayList<>(aggregate.groupBy());
        if (interval.getStep() != null) {
            // a time series query
            keys.add(TimestampSpec.COLUMN_ALIAS);
        }

        return new JdbcReadStep(dslContext,
                                schema,
                                sqlDialect,
                                select,
                                interval,
                                keys,
                                List.of(aggregate.field().getName()));
    }

    @Override
    public CompletableFuture<PipelineQueryResult> execute() throws Exception {
        return CompletableFuture.supplyAsync(() -> {
            ColumnarTable resultTable = new ColumnarTable();
            for (Selector selector : selectStatement.getSelectorList().getSelectors()) {
                resultTable.addColumn(Column.create(selector.getOutputName(), selector.getDataType(), 256));
            }
            List<Column> resultColumns = resultTable.getColumns();

            log.info("Executing {}", sql);
            try (Cursor<org.jooq.Record> cursor = dslContext.fetchLazy(sql)) {
                for (Record record : cursor) {
                    for (int i = 0; i < resultColumns.size(); i++) {
                        Column column = resultColumns.get(i);
                        column.addObject(record.get(i));
                    }
                }
                return PipelineQueryResult.builder()
                                          .rows(resultTable.rowCount())
                                          .table(resultTable)
                                          .keyColumns(this.keyColumns)
                                          .valColumns(this.valColumns)
                                          .build();
            }
        });
    }
}
