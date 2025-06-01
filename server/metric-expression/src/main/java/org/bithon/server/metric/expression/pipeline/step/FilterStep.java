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
import org.bithon.server.datasource.query.pipeline.IQueryStep;
import org.bithon.server.datasource.query.pipeline.PipelineQueryResult;

import java.util.concurrent.CompletableFuture;

/**
 * @author frank.chen021@outlook.com
 * @date 1/6/25 9:06 pm
 */
public abstract class FilterStep implements IQueryStep {
    protected final IQueryStep source;
    protected final LiteralExpression<?> expected;

    protected FilterStep(IQueryStep source, LiteralExpression<?> expected) {
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

                         int selectionIndex = 0;
                         int[] selection = new int[result.getTable().rowCount()];
                         if (column.getDataType() == IDataType.LONG) {
                             long expectedValue = ((Number) expected.getValue()).longValue();

                             for (int i = 0, size = column.size(); i < size; i++) {
                                 if (filter(column.getLong(i), expectedValue)) {
                                     selection[selectionIndex++] = i;
                                 }
                             }
                         } else if (column.getDataType() == IDataType.DOUBLE) {
                             double expectedValue = ((Number) expected.getValue()).doubleValue();

                             for (int i = 0, size = column.size(); i < size; i++) {
                                 if (filter(column.getDouble(i), expectedValue)) {
                                     selection[selectionIndex++] = i;
                                 }
                             }
                         } else {
                             throw new UnsupportedOperationException("Unsupported data type: " + column.getDataType());
                         }
                         result.getTable().view(selection, selectionIndex);

                         return PipelineQueryResult.builder()
                                                   .rows(selectionIndex)
                                                   .keyColumns(result.getKeyColumns())
                                                   .valColumns(result.getValColumns())
                                                   .table(result.getTable().view(selection, selectionIndex))
                                                   .build();
                     });
    }

    protected abstract boolean filter(long actual, long expected);

    protected abstract boolean filter(double actual, double expected);

    public static class LT extends FilterStep {
        public LT(IQueryStep source, LiteralExpression<?> expected) {
            super(source, expected);
        }

        @Override
        protected boolean filter(long actual, long expected);
        // Implement filtering logic for LT
    }
}
}
