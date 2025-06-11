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

package org.bithon.server.datasource.query.plan.physical;


import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.query.Interval;
import org.bithon.server.datasource.query.result.Column;
import org.bithon.server.datasource.query.result.ColumnarTable;
import org.bithon.server.datasource.query.result.DoubleColumn;
import org.bithon.server.datasource.query.result.LongColumn;
import org.bithon.server.datasource.query.result.PipelineQueryResult;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Produce the literal value as a scalar result set.
 *
 * @author frank.chen021@outlook.com
 * @date 4/4/25 9:36 pm
 */
public class LiteralQueryStep implements IPhysicalPlan {
    private final LiteralExpression<?> expression;
    private final int size;

    public LiteralQueryStep(LiteralExpression<?> expression) {
        this.expression = expression;
        this.size = 1;
    }

    public LiteralQueryStep(LiteralExpression<?> expression, Interval interval) {
        this.expression = expression;

        TimeSpan start = interval.getStartTime();
        TimeSpan end = interval.getEndTime();
        long intervalLength = (end.getMilliseconds() - start.getMilliseconds()) / 1000;
        if (interval.getStep() != null) {
            size = (int) (intervalLength / interval.getStep().getSeconds());
        } else {
            size = 1;
        }
    }

    @Override
    public void serializer(PhysicalPlanSerializer serializer) {
        serializer.append(expression.serializeToText());
    }

    @Override
    public boolean isScalar() {
        return size == 1;
    }

    @Override
    public CompletableFuture<PipelineQueryResult> execute() {

        ColumnarTable table = new ColumnarTable();
        if (expression.getDataType() == IDataType.LONG) {
            Column column = table.addColumn(new LongColumn("value", size));

            long val = ((Number) expression.getValue()).longValue();
            for (int i = 0; i < size; i++) {
                column.addLong(val);
            }
        } else if (expression.getDataType() == IDataType.DOUBLE) {
            Column column = table.addColumn(new DoubleColumn("value", size));

            double val = ((Number) expression.getValue()).doubleValue();
            for (int i = 0; i < size; i++) {
                column.addDouble(val);
            }
        } else {
            throw new IllegalStateException("Unsupported literal type: " + expression.getDataType());
        }

        return CompletableFuture.completedFuture(PipelineQueryResult.builder()
                                                                    .rows(1)
                                                                    .keyColumns(Collections.emptyList())
                                                                    .valColumns(List.of("value"))
                                                                    .table(table)
                                                                    .build());
    }
}
