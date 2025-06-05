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

import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.datasource.query.ast.ExpressionNode;
import org.bithon.server.datasource.query.ast.Selector;
import org.bithon.server.datasource.query.plan.logical.ILogicalPlan;
import org.bithon.server.datasource.query.plan.logical.LogicalAggregate;
import org.bithon.server.datasource.query.plan.logical.LogicalTableScan;
import org.bithon.server.datasource.query.plan.physical.IPhysicalPlan;
import org.bithon.server.datasource.query.plan.physical.PhysicalPlanner;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.pipeline.JdbcTableScanStep;
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

    public JdbcPhysicalPlanner(DSLContext dslContext, ISqlDialect sqlDialect) {
        this.dslContext = dslContext;
        this.sqlDialect = sqlDialect;
    }

    public IPhysicalPlan plan(ILogicalPlan logicalPlan) {
        return logicalPlan.accept(this);
    }

    @Override
    public IPhysicalPlan visitAggregate(LogicalAggregate aggregate) {
        IPhysicalPlan physicalPlan = aggregate.input().accept(this);
        if (physicalPlan instanceof JdbcTableScanStep tableScan) {
            // PUSH the aggregate down to the table scan step
            //JdbcTableScanStep tableScanStep
            return new JdbcTableScanStep(dslContext,
                                         sqlDialect,
                                         false,
                                         plan(sqlDialect, tableScan.getSelectStatement(), aggregate));
        }

        return super.visitAggregate(aggregate);
    }

    @Override
    public IPhysicalPlan visitTableScan(LogicalTableScan tableScan) {
        SelectStatement selectStatement = new SelectStatement();
        selectStatement.getFrom().setExpression(new TableIdentifier(tableScan.table()));
        selectStatement.getFrom().setAlias(tableScan.table());
        selectStatement.getWhere().and(tableScan.filter());
        selectStatement.getSelectorList().addAll(tableScan.selectorList());

        return new JdbcTableScanStep(
            dslContext,
            sqlDialect,
            false,
            selectStatement
        );
    }

    private SelectStatement plan(ISqlDialect sqlDialect, SelectStatement select, LogicalAggregate aggregate) {
        SelectStatementBuilder builder = new SelectStatementBuilder();
        return builder.sqlDialect(sqlDialect)
                      .fields(List.of(new Selector(new ExpressionNode(StringUtils.format("%s(%s)", aggregate.func(), aggregate.field().getName())),
                                                   aggregate.field().getName(),
                                                   aggregate.field().getDataType())))
                      .filter(LogicalExpression.create(LogicalExpression.AND.OP, select.getWhere().getExpressions()))
                      .groupBy(aggregate.groupBy())
                      .build();
    }
}
