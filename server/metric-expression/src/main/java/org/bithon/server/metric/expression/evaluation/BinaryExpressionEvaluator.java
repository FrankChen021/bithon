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

package org.bithon.server.metric.expression.evaluation;


import org.bithon.component.commons.expression.IDataType;
import org.bithon.server.metric.expression.format.Column;
import org.bithon.server.metric.expression.format.ColumnOperator;
import org.bithon.server.metric.expression.format.ColumnarTable;
import org.bithon.server.metric.expression.format.HashJoiner;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author frank.chen021@outlook.com
 * @date 4/4/25 3:49 pm
 */
public abstract class BinaryExpressionEvaluator implements IEvaluator {
    private final IEvaluator lhs;
    private final IEvaluator rhs;

    public static class Add extends BinaryExpressionEvaluator {
        public Add(IEvaluator left, IEvaluator right) {
            super(left, right, null);
        }

        public Add(IEvaluator left,
                   IEvaluator right,
                   String resultColumn,
                   String... sourceColumns) {
            super(left, right, resultColumn, sourceColumns);
        }

        @Override
        double apply(double l, double r) {
            return l + r;
        }

        @Override
        int getOperatorIndex() {
            return 0;
        }
    }

    public static class Sub extends BinaryExpressionEvaluator {
        public Sub(IEvaluator left, IEvaluator right) {
            super(left, right, null);
        }

        public Sub(IEvaluator left,
                   IEvaluator right,
                   String resultColumn,
                   String... sourceColumns) {
            super(left, right, resultColumn, sourceColumns);
        }

        @Override
        double apply(double l, double r) {
            return l - r;
        }

        @Override
        int getOperatorIndex() {
            return 1;
        }
    }

    public static class Mul extends BinaryExpressionEvaluator {
        public Mul(IEvaluator left, IEvaluator right) {
            super(left, right, null);
        }

        public Mul(IEvaluator left,
                   IEvaluator right,
                   String resultColumn,
                   String... sourceColumns) {
            super(left, right, resultColumn, sourceColumns);
        }

        @Override
        double apply(double l, double r) {
            return l * r;
        }

        @Override
        int getOperatorIndex() {
            return 2;
        }
    }

    public static class Div extends BinaryExpressionEvaluator {
        public Div(IEvaluator left, IEvaluator right) {
            super(left, right, null);
        }

        public Div(IEvaluator left,
                   IEvaluator right,
                   String resultColumn,
                   String... sourceColumns) {
            super(left, right, resultColumn, sourceColumns);
        }

        @Override
        double apply(double l, double r) {
            return l / r;
        }

        @Override
        int getOperatorIndex() {
            return 3;
        }
    }

    protected BinaryExpressionEvaluator(IEvaluator left,
                                        IEvaluator right,
                                        String resultColumnName,
                                        String... sourceColumns) {
        this.lhs = left;
        this.rhs = right;
    }

    @Override
    public CompletableFuture<EvaluationResult> evaluate() throws Exception {
        CompletableFuture<EvaluationResult> leftFuture = this.lhs.evaluate();
        CompletableFuture<EvaluationResult> rightFuture = this.rhs.evaluate();

        return CompletableFuture.allOf(leftFuture, rightFuture)
                                .thenApply(v -> {
                                    try {
                                        EvaluationResult l = leftFuture.get();
                                        EvaluationResult r = rightFuture.get();
                                        EvaluationResult result;
                                        if (lhs.isScalar()) {
                                            if (rhs.isScalar()) {
                                                result = applyScalarOverScalar(l, r);
                                            } else {
                                                result = applyScalarOverVector(l, r);
                                            }
                                        } else {
                                            if (rhs.isScalar()) {
                                                result = applyVectorOverScalar(l, r);
                                            } else {
                                                result = applyVectorOverVector(l, r);
                                            }
                                        }
                                        return transformResult(result, l, r);
                                    } catch (InterruptedException | ExecutionException e) {
                                        throw new RuntimeException(e);
                                    }
                                });
    }

    @Override
    public boolean isScalar() {
        return this.lhs.isScalar() && this.rhs.isScalar();
    }

    abstract double apply(double l, double r);

    abstract int getOperatorIndex();

    private EvaluationResult transformResult(EvaluationResult result, EvaluationResult lResponse, EvaluationResult rResponse) {
        return result;
    }

    private EvaluationResult applyScalarOverScalar(EvaluationResult left,
                                                   EvaluationResult right) {
        String leftColName = left.getValColumns().get(0);
        Column leftColumn = left.getTable().getColumn(leftColName);

        String rightColName = right.getValColumns().get(0);
        Column rightColumn = right.getTable().getColumn(rightColName);

        Column result = ColumnOperator.ScalarOverScalarOperator.apply(leftColumn, rightColumn, "value", this.getOperatorIndex());

        return EvaluationResult.builder()
                               .startTimestamp(left.getStartTimestamp())
                               .endTimestamp(left.getEndTimestamp())
                               .interval(left.getInterval())
                               .table(ColumnarTable.of("value", result))
                               .build();
    }

    private EvaluationResult applyScalarOverVector(EvaluationResult left, EvaluationResult right) {
        ColumnarTable table = new ColumnarTable();

        // Retain all key columns on the right, which is the vector
        for (String colName : right.getKeyColumns()) {
            Column col = right.getTable().getColumn(colName);
            table.addColumn(col);
        }

        //
        // Apply operator on ALL value columns on the right
        //
        String lName = left.getValColumns().get(0);
        Column lColumn = left.getTable().getColumn(lName);

        for (String colName : right.getValColumns()) {
            Column col = right.getTable().getColumn(colName);
            Column result = ColumnOperator.ScalarOverVectorOperator.apply(lColumn, col, colName, this.getOperatorIndex());
            table.addColumn(result);
        }

        return EvaluationResult.builder()
                               .startTimestamp(left.getStartTimestamp())
                               .endTimestamp(left.getEndTimestamp())
                               .interval(left.getInterval())
                               .table(table)
                               .build();
    }

    private EvaluationResult applyVectorOverScalar(EvaluationResult left, EvaluationResult right) {
        ColumnarTable table = new ColumnarTable();

        // Retain all key columns on the right, which is the vector
        for (String colName : left.getKeyColumns()) {
            Column col = left.getTable().getColumn(colName);
            table.addColumn(col);
        }

        //
        // Apply operator on ALL value columns on the right
        //
        String rName = right.getValColumns().get(0);
        Column rColumn = right.getTable().getColumn(rName);

        for (String colName : left.getValColumns()) {
            Column col = left.getTable().getColumn(colName);
            Column result = ColumnOperator.VectorOverScalarOperator.apply(col, rColumn, colName, this.getOperatorIndex());
            table.addColumn(result);
        }

        return EvaluationResult.builder()
                               .startTimestamp(left.getStartTimestamp())
                               .endTimestamp(left.getEndTimestamp())
                               .interval(left.getInterval())
                               .table(table)
                               .build();
    }

    /**
     * join these two maps by its keyNames
     */
    private EvaluationResult applyVectorOverVector(EvaluationResult left, EvaluationResult right) {
        if (!left.getKeyColumns().equals(right.getKeyColumns())) {
            return EvaluationResult.builder()
                                   // create an EMPTY table
                                   .table(new ColumnarTable())
                                   .build();
        }

        // Join ALL columns together
        List<Column> columns = HashJoiner.hashJoin(left.getTable(),
                                                   right.getTable(),
                                                   left.getKeyColumns(),
                                                   left.getValColumns().stream().map((col) -> left.getTable().getColumn(col)).toList(),
                                                   right.getValColumns().stream().map((col) -> right.getTable().getColumn(col)).toList());

        // Apply operator on target columns
        int leftColIndex = left.getKeyColumns().size();
        int rightColIndex = leftColIndex + left.getValColumns().size();
        Column result = ColumnOperator.VectorOverVectorOperator.apply(columns.get(leftColIndex), columns.get(rightColIndex), "value", this.getOperatorIndex());

        ColumnarTable table = new ColumnarTable();
        int i = 0;
        for (; i < left.getKeyColumns().size(); i++) {
            table.addColumn(columns.get(i));
        }

        table.addColumn(result);

        for (
            // Skip the first value column on the left column
            i++;
            i < left.getValColumns().size();
            i++) {
            table.addColumn(columns.get(i));
        }
        for (
            // Skip the first value column on the right column
            i++;
            i < left.getValColumns().size();
            i++) {
            table.addColumn(columns.get(i));
        }

        return EvaluationResult.builder()
                               .interval(left.getInterval())
                               .startTimestamp(left.getStartTimestamp())
                               .endTimestamp(left.getEndTimestamp())
                               .table(table)
                               .build();
    }

    interface Operator {
        void apply(Column left, Column right, Column output, int row);
    }

    private void apply(Column left, Column right, Column output, int row) {

    }

    IDataType determineTargetColumnType(IDataType left, IDataType right) {
        if (left == IDataType.STRING || right == IDataType.STRING) {
            return IDataType.STRING;
        } else if (left == IDataType.DOUBLE || right == IDataType.DOUBLE) {
            return IDataType.DOUBLE;
        } else if (left == IDataType.LONG && right == IDataType.LONG) {
            return IDataType.LONG;
        }
        throw new UnsupportedOperationException("Unsupported column type: " + left + ", " + right);
    }
}
