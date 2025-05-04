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

package org.bithon.server.metric.expression.pipeline;


import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.server.metric.expression.format.Column;
import org.bithon.server.metric.expression.format.ColumnarTable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Produce the literal value as a scalar result set.
 *
 * @author frank.chen021@outlook.com
 * @date 4/4/25 9:36 pm
 */
public class LiteralQueryStep implements IQueryStep {
    private final LiteralExpression<?> expression;

    public LiteralQueryStep(LiteralExpression<?> expression) {
        this.expression = expression;
    }

    @Override
    public boolean isScalar() {
        return true;
    }

    @Override
    public CompletableFuture<IntermediateQueryResult> execute() {

        ColumnarTable table = new ColumnarTable();
        if (expression.getDataType() == IDataType.LONG) {
            table.addColumn(new Column.LongColumn("value", new long[]{
                // Convert to Number because it might be Customized Number
                ((Number) expression.getValue()).longValue()
            }));
        } else if (expression.getDataType() == IDataType.DOUBLE) {
            table.addColumn(new Column.DoubleColumn("value", new double[]{
                // Convert to Number because it might be Customized Number
                ((Number) expression.getValue()).doubleValue()
            }));
        } else {
            throw new IllegalStateException("Unsupported literal type: " + expression.getDataType());
        }

        return CompletableFuture.completedFuture(IntermediateQueryResult.builder()
                                                                        .rows(1)
                                                                        .valColumns(List.of("value"))
                                                                        .table(table)
                                                                        .build());
    }
}
