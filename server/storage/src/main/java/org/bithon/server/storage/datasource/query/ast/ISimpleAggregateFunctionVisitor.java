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

package org.bithon.server.storage.datasource.query.ast;

/**
 * @author Frank Chen
 * @date 1/11/21 3:12 pm
 */
public interface ISimpleAggregateFunctionVisitor<T> {
    T visit(SimpleAggregateExpressions.CardinalityAggregateExpression aggregator);

    T visit(SimpleAggregateExpressions.CountAggregateExpression aggregator);

    T visit(SimpleAggregateExpressions.AvgAggregateExpression aggregator);

    T visit(SimpleAggregateExpressions.FirstAggregateExpression aggregator);

    T visit(SimpleAggregateExpressions.LastAggregateExpression aggregator);

    T visit(SimpleAggregateExpressions.RateAggregateExpression aggregator);

    T visit(SimpleAggregateExpressions.MaxAggregateExpression aggregator);

    T visit(SimpleAggregateExpressions.MinAggregateExpression aggregator);

    T visit(SimpleAggregateExpressions.SumAggregateExpression aggregator);

    T visit(SimpleAggregateExpressions.GroupConcatAggregateExpression aggregator);
}
