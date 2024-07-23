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

import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.column.DateTimeColumn;
import org.bithon.server.storage.datasource.column.ExpressionColumn;
import org.bithon.server.storage.datasource.column.IColumnVisitor;
import org.bithon.server.storage.datasource.column.LongColumn;
import org.bithon.server.storage.datasource.column.ObjectColumn;
import org.bithon.server.storage.datasource.column.StringColumn;
import org.bithon.server.storage.datasource.column.aggregatable.IAggregatableColumn;
import org.bithon.server.storage.datasource.column.aggregatable.count.AggregateCountColumn;
import org.bithon.server.storage.datasource.column.aggregatable.last.AggregateDoubleLastColumn;
import org.bithon.server.storage.datasource.column.aggregatable.last.AggregateLongLastColumn;
import org.bithon.server.storage.datasource.column.aggregatable.max.AggregateLongMaxColumn;
import org.bithon.server.storage.datasource.column.aggregatable.min.AggregateLongMinColumn;
import org.bithon.server.storage.datasource.column.aggregatable.sum.AggregateDoubleSumColumn;
import org.bithon.server.storage.datasource.column.aggregatable.sum.AggregateLongSumColumn;
import org.bithon.server.storage.jdbc.common.query.ast.SimpleAggregateExpression;
import org.bithon.server.storage.jdbc.common.query.ast.SimpleAggregateExpressions;

/**
 * @author frank.chen021@outlook.com
 * @date 23/7/24 1:43 pm
 */
public class AggregatorFactory {
    public static SimpleAggregateExpression create(IAggregatableColumn column) {
        return column.accept(new Visitor());
    }

    private static class Visitor implements IColumnVisitor<SimpleAggregateExpression> {
        @Override
        public SimpleAggregateExpression visit(LongColumn column) {
            throw new UnsupportedOperationException(StringUtils.format("getAggregateExpression is not supported on type of " + this.getClass().getSimpleName()));
        }

        @Override
        public SimpleAggregateExpression visit(ObjectColumn column) {
            throw new UnsupportedOperationException(StringUtils.format("getAggregateExpression is not supported on type of " + this.getClass().getSimpleName()));
        }

        @Override
        public SimpleAggregateExpression visit(StringColumn column) {
            throw new UnsupportedOperationException(StringUtils.format("getAggregateExpression is not supported on type of " + this.getClass().getSimpleName()));
        }

        @Override
        public SimpleAggregateExpression visit(DateTimeColumn column) {
            throw new UnsupportedOperationException(StringUtils.format("getAggregateExpression is not supported on type of " + this.getClass().getSimpleName()));
        }

        @Override
        public SimpleAggregateExpression visit(ExpressionColumn column) {
            return null;
        }

        @Override
        public SimpleAggregateExpression visit(AggregateDoubleSumColumn column) {
            return new SimpleAggregateExpressions.SumAggregateExpression(column.getName());
        }

        @Override
        public SimpleAggregateExpression visit(AggregateLongSumColumn column) {
            return new SimpleAggregateExpressions.SumAggregateExpression(column.getName());
        }

        @Override
        public SimpleAggregateExpression visit(AggregateLongMinColumn column) {
            return new SimpleAggregateExpressions.MinAggregateExpression(column.getName());
        }

        @Override
        public SimpleAggregateExpression visit(AggregateDoubleLastColumn column) {
            return new SimpleAggregateExpressions.LastAggregateExpression(column.getName());
        }

        @Override
        public SimpleAggregateExpression visit(AggregateLongMaxColumn column) {
            return new SimpleAggregateExpressions.MaxAggregateExpression(column.getName());
        }

        @Override
        public SimpleAggregateExpression visit(AggregateLongLastColumn column) {
            return new SimpleAggregateExpressions.LastAggregateExpression(column.getName());
        }

        @Override
        public SimpleAggregateExpression visit(AggregateCountColumn column) {
            return new SimpleAggregateExpressions.CountAggregateExpression(column.getName());
        }
    }
}
