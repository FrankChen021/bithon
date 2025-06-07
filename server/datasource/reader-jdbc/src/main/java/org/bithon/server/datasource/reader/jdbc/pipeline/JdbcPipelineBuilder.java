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


import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.datasource.query.Interval;
import org.bithon.server.datasource.query.Order;
import org.bithon.server.datasource.query.ast.ExpressionNode;
import org.bithon.server.datasource.query.ast.Selector;
import org.bithon.server.datasource.query.plan.physical.IPhysicalPlan;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.statement.ast.OrderByClause;
import org.bithon.server.datasource.reader.jdbc.statement.ast.SelectStatement;
import org.bithon.server.datasource.reader.jdbc.statement.ast.WindowFunctionExpression;
import org.jooq.DSLContext;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 5/5/25 6:43 pm
 */
public class JdbcPipelineBuilder {
    private DSLContext dslContext;
    private ISqlDialect dialect;
    private SelectStatement selectStatement;
    private Interval interval;

    private JdbcPipelineBuilder() {
    }

    public static JdbcPipelineBuilder builder() {
        return new JdbcPipelineBuilder();
    }

    public JdbcPipelineBuilder dslContext(DSLContext dslContext) {
        this.dslContext = dslContext;
        return this;
    }

    public JdbcPipelineBuilder dialect(ISqlDialect dialect) {
        this.dialect = dialect;
        return this;
    }

    public JdbcPipelineBuilder selectStatement(SelectStatement selectStatement) {
        this.selectStatement = selectStatement;
        return this;
    }


    public JdbcPipelineBuilder interval(Interval interval) {
        this.interval = interval;
        return this;
    }

    public IPhysicalPlan build() {
        List<Selector> windowFunctionSelectors = new ArrayList<>();
        List<String> inputColumns = new ArrayList<>();
        List<String> outputColumns = new ArrayList<>();

        for (int i = 0, size = selectStatement.getSelectorList().size(); i < size; i++) {
            Selector selector = selectStatement.getSelectorList().get(i);
            if (selector.getSelectExpression() instanceof ExpressionNode expression) {
                if (expression.getParsedExpression() instanceof WindowFunctionExpression windowFunction) {
                    windowFunctionSelectors.add(selector);

                    inputColumns.add(((IdentifierExpression) windowFunction.getArgs().get(0)).getIdentifier());
                    outputColumns.add(selector.getOutputName());
                }
            }
        }

        if (windowFunctionSelectors.isEmpty()) {
            return new JdbcReadStep(dslContext, dialect, selectStatement);
        }

        WindowFunctionExpression windowFunctionExpression = (WindowFunctionExpression) ((ExpressionNode) windowFunctionSelectors.get(0).getSelectExpression()).getParsedExpression();
        LiteralExpression.LongLiteral window = (LiteralExpression.LongLiteral) windowFunctionExpression.getFrame().getStart();
        IdentifierExpression orderBy = (IdentifierExpression) windowFunctionExpression.getOrderBy()[0].getName();

        List<String> keys = CollectionUtils.isEmpty(windowFunctionExpression.getPartitionBy()) ?
            Collections.emptyList()
            : Arrays.stream(windowFunctionExpression.getPartitionBy())
                    .map((expr) -> ((IdentifierExpression) expr).getIdentifier())
                    .toList();

        SelectStatement subQuery = (SelectStatement) selectStatement.getFrom().getExpression();

        // Set ORDER BY of the sub query to ensure the order of the result set satisfies the requirement of the following window aggregation
        List<String> orderFields = new ArrayList<>(keys);
        orderFields.add(orderBy.getIdentifier());
        subQuery.setOrderBy(orderFields.stream()
                                       .map((o) -> new OrderByClause(o, Order.asc))
                                       .toList()
                                       .toArray(new OrderByClause[0]));

        IPhysicalPlan readStep = new JdbcReadStep(dslContext,
                                                  dialect,
                                                  subQuery
        );

        return new SlidingWindowAggregationStep(orderBy.getIdentifier(),
                                                keys,
                                                inputColumns,
                                                outputColumns,
                                                Duration.ofSeconds(window.getValue()),
                                                this.interval,
                                                readStep);
    }
}
