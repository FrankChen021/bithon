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

package org.bithon.server.metric.expression.pipeline.step;


import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.server.datasource.query.pipeline.Column;
import org.bithon.server.datasource.query.pipeline.ColumnarTable;
import org.bithon.server.datasource.query.pipeline.IQueryStep;
import org.bithon.server.datasource.query.pipeline.PipelineQueryResult;
import org.bithon.server.metric.expression.ast.MetricExpectedExpression;

import java.util.concurrent.CompletableFuture;

/**
 * @author frank.chen021@outlook.com
 * @date 1/6/25 9:06 pm
 */
public abstract class FilterStep implements IQueryStep {
    protected final IQueryStep source;
    protected final MetricExpectedExpression expected;

    protected FilterStep(IQueryStep source, MetricExpectedExpression expected) {
        this.source = source;
        this.expected = expected;
    }

    @Override
    public boolean isScalar() {
        return source.isScalar();
    }

    @Override
    public CompletableFuture<PipelineQueryResult> execute() throws Exception {
        return source.execute()
                     .thenApply(result -> {
                         String valColumn = result.getValColumns().get(0);
                         Column column = result.getTable().getColumn(valColumn);

                         int filteredRowCount = 0;
                         int[] filteredRows = new int[result.getTable().rowCount()];
                         if (column.getDataType() == IDataType.LONG) {
                             long expectedValue = ((Number) expected.getExpected().getValue()).longValue();

                             for (int i = 0, size = column.size(); i < size; i++) {
                                 if (filter(column.getLong(i), expectedValue)) {
                                     filteredRows[filteredRowCount++] = i;
                                 }
                             }
                         } else if (column.getDataType() == IDataType.DOUBLE) {
                             double expectedValue = ((Number) expected.getExpected().getValue()).doubleValue();

                             for (int i = 0, size = column.size(); i < size; i++) {
                                 if (filter(column.getDouble(i), expectedValue)) {
                                     filteredRows[filteredRowCount++] = i;
                                 }
                             }
                         } else {
                             throw new UnsupportedOperationException("Unsupported data type: " + column.getDataType());
                         }

                         if (filteredRowCount < filteredRows.length) {
                             ColumnarTable newTable = result.getTable()
                                                            .view(filteredRows, filteredRowCount);

                             return PipelineQueryResult.builder()
                                                       .rows(filteredRowCount)
                                                       .table(newTable)
                                                       .keyColumns(result.getKeyColumns())
                                                       .valColumns(result.getValColumns())
                                                       .build();
                         } else {
                             // All results are filtered out, no need to apply a filter view on top of it
                             return result;
                         }
                     });
    }

    protected abstract boolean filter(long actual, long expected);

    protected abstract boolean filter(double actual, double expected);

    public static class LT extends FilterStep {
        public LT(IQueryStep source, MetricExpectedExpression expected) {
            super(source, expected);
        }

        @Override
        protected boolean filter(long actual, long expected) {
            return actual < expected;
        }

        @Override
        protected boolean filter(double actual, double expected) {
            return actual < expected;
        }
    }

    public static class LTE extends FilterStep {
        public LTE(IQueryStep source, MetricExpectedExpression expected) {
            super(source, expected);
        }

        @Override
        protected boolean filter(long actual, long expected) {
            return actual <= expected;
        }

        @Override
        protected boolean filter(double actual, double expected) {
            return actual <= expected;
        }
    }

    public static class GT extends FilterStep {
        public GT(IQueryStep source, MetricExpectedExpression expected) {
            super(source, expected);
        }

        @Override
        protected boolean filter(long actual, long expected) {
            return actual > expected;
        }

        @Override
        protected boolean filter(double actual, double expected) {
            return actual > expected;
        }
    }

    public static class GTE extends FilterStep {
        public GTE(IQueryStep source, MetricExpectedExpression expected) {
            super(source, expected);
        }

        @Override
        protected boolean filter(long actual, long expected) {
            return actual >= expected;
        }

        @Override
        protected boolean filter(double actual, double expected) {
            return actual >= expected;
        }
    }

    public static class NE extends FilterStep {
        public NE(IQueryStep source, MetricExpectedExpression expected) {
            super(source, expected);
        }

        @Override
        protected boolean filter(long actual, long expected) {
            return actual != expected;
        }

        @Override
        protected boolean filter(double actual, double expected) {
            return actual != expected;
        }
    }
}
