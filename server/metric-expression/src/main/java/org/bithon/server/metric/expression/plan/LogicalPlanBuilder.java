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

package org.bithon.server.metric.expression.plan;

import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.datasource.query.ast.Selector;
import org.bithon.server.datasource.query.plan.logical.ILogicalPlan;
import org.bithon.server.datasource.query.plan.logical.LogicalAggregate;
import org.bithon.server.datasource.query.plan.logical.LogicalTableScan;
import org.bithon.server.metric.expression.ast.IMetricExpressionVisitor;
import org.bithon.server.metric.expression.ast.MetricAggregateExpression;
import org.bithon.server.metric.expression.ast.MetricSelectExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/6/4 23:43
 */
class LogicalPlanBuilder implements IMetricExpressionVisitor<ILogicalPlan> {
    @Override
    public ILogicalPlan visit(MetricSelectExpression expression) {
        return new LogicalTableScan(
            expression.getFrom(),
            List.of(new Selector(expression.getMetric(), expression.getMetric(), expression.getDataType())),
            expression.getLabelSelectorExpression()
        );
    }

    @Override
    public ILogicalPlan visit(MetricAggregateExpression expression) {
        return new LogicalAggregate(
            expression.getMetric().getAggregator(),
            new LogicalTableScan(expression.getFrom(),
                                 List.of(new Selector(expression.getMetric().getName(),
                                                      expression.getMetric().getName(),
                                                      expression.getDataType())),
                                 expression.getLabelSelectorExpression()),
            null,
            CollectionUtils.isEmpty(expression.getGroupBy()) ? new ArrayList<>() : new ArrayList<>(expression.getGroupBy()),
            new ArrayList<>()
        );
    }
}
