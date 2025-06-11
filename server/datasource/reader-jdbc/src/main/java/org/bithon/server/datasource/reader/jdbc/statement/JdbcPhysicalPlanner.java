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

package org.bithon.server.datasource.reader.jdbc.statement;

import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.query.Interval;
import org.bithon.server.datasource.query.ast.ExpressionNode;
import org.bithon.server.datasource.query.ast.Selector;
import org.bithon.server.datasource.query.plan.logical.ILogicalPlan;
import org.bithon.server.datasource.query.plan.logical.LogicalAggregate;
import org.bithon.server.datasource.query.plan.logical.LogicalBinaryOp;
import org.bithon.server.datasource.query.plan.logical.LogicalScalar;
import org.bithon.server.datasource.query.plan.logical.LogicalTableScan;
import org.bithon.server.datasource.query.plan.physical.ArithmeticStep;
import org.bithon.server.datasource.query.plan.physical.IPhysicalPlan;
import org.bithon.server.datasource.query.plan.physical.LiteralQueryStep;
import org.bithon.server.datasource.query.plan.physical.PhysicalPlanner;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.pipeline.JdbcReadStep;
import org.bithon.server.datasource.reader.jdbc.statement.ast.SelectStatement;
import org.bithon.server.datasource.reader.jdbc.statement.ast.TableIdentifier;
import org.bithon.server.datasource.reader.jdbc.statement.builder.SelectStatementBuilder;
import org.jooq.DSLContext;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/6/5 20:14
 */
public class JdbcPhysicalPlanner extends PhysicalPlanner {
    private final DSLContext dslContext;
    private final ISqlDialect sqlDialect;
    private final ISchema schema;
    private Interval interval;

    public JdbcPhysicalPlanner(DSLContext dslContext,
                               ISqlDialect sqlDialect,
                               ISchema schema) {
        this.dslContext = dslContext;
        this.sqlDialect = sqlDialect;
        this.schema = schema;
    }

    public IPhysicalPlan plan(Interval interval, ILogicalPlan logicalPlan) {
        this.interval = interval;
        return logicalPlan.accept(this);
    }

    @Override
    public IPhysicalPlan visitAggregate(LogicalAggregate aggregate) {
        IPhysicalPlan physicalPlan = aggregate.input().accept(this);
        if (physicalPlan instanceof JdbcReadStep jdbcReadStep) {
            // PUSH the aggregate down to the table scan step
            SelectStatement aggregation = pushdownAggregationOverScan(
                jdbcReadStep.getSelectStatement(),
                aggregate
            );

            return new JdbcReadStep(dslContext,
                                    sqlDialect,
                                    aggregation);
        }

        return super.visitAggregate(aggregate);
    }

    @Override
    public IPhysicalPlan visitTableScan(LogicalTableScan tableScan) {
        SelectStatement selectStatement = new SelectStatement();
        selectStatement.getFrom().setExpression(new TableIdentifier(tableScan.table()));
        selectStatement.getFrom().setAlias(tableScan.table());
        selectStatement.getWhere().and(
            new ComparisonExpression.GTE(this.interval.getTimestampColumn(), sqlDialect.toISO8601TimestampExpression(this.interval.getStartTime())),
            new ComparisonExpression.LT(this.interval.getTimestampColumn(), sqlDialect.toISO8601TimestampExpression(this.interval.getEndTime())),
            tableScan.filter());
        selectStatement.getSelectorList().addAll(tableScan.selectorList());

        return new JdbcReadStep(
            dslContext,
            sqlDialect,
            selectStatement
        );
    }

    @Override
    public IPhysicalPlan visitBinaryOp(LogicalBinaryOp binaryOp) {
        return switch (binaryOp.op()) {
            case ADD -> new ArithmeticStep.Add(binaryOp.left().accept(this),
                                               binaryOp.right().accept(this));
            case SUB -> new ArithmeticStep.Sub(binaryOp.left().accept(this),
                                               binaryOp.right().accept(this));
            case MUL -> new ArithmeticStep.Mul(binaryOp.left().accept(this),
                                               binaryOp.right().accept(this));
            case DIV -> new ArithmeticStep.Div(binaryOp.left().accept(this),
                                               binaryOp.right().accept(this));
        };
    }

    @Override
    public IPhysicalPlan visitScalar(LogicalScalar scalar) {
        return new LiteralQueryStep(scalar.literal());
    }

    private SelectStatement pushdownAggregationOverScan(SelectStatement select, LogicalAggregate aggregate) {
        SelectStatementBuilder builder = new SelectStatementBuilder();
        return builder.sqlDialect(this.sqlDialect)
                      .schema(this.schema)
                      .interval(this.interval)
                      .fields(List.of(new Selector(new ExpressionNode(this.schema,
                                                                      StringUtils.format("%s(%s)", aggregate.func(), aggregate.field().getName())),
                                                   aggregate.field().getName(),
                                                   aggregate.field().getDataType())))
                      .filter(LogicalExpression.create(LogicalExpression.AND.OP, select.getWhere().getExpressions()))
                      .groupBy(aggregate.groupBy())
                      .build();
    }
}
