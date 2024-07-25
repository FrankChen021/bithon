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
public interface IQueryAggregateFunctionVisitor<T> {
    T visit(QueryAggregateFunction.Cardinality aggregator);

    T visit(QueryAggregateFunction.Count aggregator);

    T visit(QueryAggregateFunction.Avg aggregator);

    T visit(QueryAggregateFunction.First aggregator);

    T visit(QueryAggregateFunction.Last aggregator);

    T visit(QueryAggregateFunction.Rate aggregator);

    T visit(QueryAggregateFunction.Max aggregator);

    T visit(QueryAggregateFunction.Min aggregator);

    T visit(QueryAggregateFunction.Sum aggregator);

    T visit(QueryAggregateFunction.GroupConcat aggregator);
}
