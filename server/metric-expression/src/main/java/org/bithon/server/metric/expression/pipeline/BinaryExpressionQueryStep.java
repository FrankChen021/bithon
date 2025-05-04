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


import org.bithon.server.metric.expression.format.Column;
import org.bithon.server.metric.expression.format.ColumnOperator;
import org.bithon.server.metric.expression.format.ColumnarTable;
import org.bithon.server.metric.expression.format.HashJoiner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author frank.chen021@outlook.com
 * @date 4/4/25 3:49 pm
 */
public abstract class BinaryExpressionQueryStep implements IQueryStep {
    private final IQueryStep lhs;
    private final IQueryStep rhs;
    private final String resultColumnName;
    /**
     * Extra columns that should be retained in the result set after operation apply
     */
    private final Set<String> retainedColumns;

    public static class Add extends BinaryExpressionQueryStep {
        public Add(IQueryStep left, IQueryStep right) {
            super(left, right, null);
        }

        public Add(IQueryStep left,
                   IQueryStep right,
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

    public static class Sub extends BinaryExpressionQueryStep {
        public Sub(IQueryStep left, IQueryStep right) {
            super(left, right, null);
        }

        public Sub(IQueryStep left,
                   IQueryStep right,
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

    public static class Mul extends BinaryExpressionQueryStep {
        public Mul(IQueryStep left, IQueryStep right) {
            super(left, right, null);
        }

        public Mul(IQueryStep left,
                   IQueryStep right,
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

    public static class Div extends BinaryExpressionQueryStep {
        public Div(IQueryStep left, IQueryStep right) {
            super(left, right, null);
        }

        public Div(IQueryStep left,
                   IQueryStep right,
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

    protected BinaryExpressionQueryStep(IQueryStep left,
                                        IQueryStep right,
                                        String resultColumnName,
                                        String... retainedColumns) {
        this.lhs = left;
        this.rhs = right;
        this.resultColumnName = resultColumnName == null ? "value" : resultColumnName;
        this.retainedColumns = retainedColumns == null ? Collections.emptySet() : new LinkedHashSet<>(Arrays.asList(retainedColumns));
    }

    @Override
    public CompletableFuture<IntermediateQueryResult> execute() throws Exception {
        CompletableFuture<IntermediateQueryResult> leftFuture = this.lhs.execute();
        CompletableFuture<IntermediateQueryResult> rightFuture = this.rhs.execute();

        return CompletableFuture.allOf(leftFuture, rightFuture)
                                .thenApply(v -> {
                                    try {
                                        IntermediateQueryResult l = leftFuture.get();
                                        IntermediateQueryResult r = rightFuture.get();
                                        IntermediateQueryResult result;
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
                                        return result;
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

    private IntermediateQueryResult applyScalarOverScalar(IntermediateQueryResult left,
                                                          IntermediateQueryResult right) {
        String leftColName = left.getValColumns().get(0);
        Column leftColumn = left.getTable().getColumn(leftColName);

        String rightColName = right.getValColumns().get(0);
        Column rightColumn = right.getTable().getColumn(rightColName);

        Column result = ColumnOperator.ScalarOverScalarOperator.apply(leftColumn, rightColumn, this.resultColumnName, this.getOperatorIndex());

        return IntermediateQueryResult.builder()
                                      .startTimestamp(left.getStartTimestamp())
                                      .endTimestamp(left.getEndTimestamp())
                                      .interval(left.getInterval())
                                      .table(ColumnarTable.of(this.resultColumnName, result))
                                      .rows(result.size())
                                      .keyColumns(left.getKeyColumns())
                                      .valColumns(List.of(this.resultColumnName))
                                      .build();
    }

    private IntermediateQueryResult applyScalarOverVector(IntermediateQueryResult left, IntermediateQueryResult right) {
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
            Column result = ColumnOperator.ScalarOverVectorOperator.apply(lColumn, col, col.getName(), this.getOperatorIndex());
            table.addColumn(result);
        }

        return IntermediateQueryResult.builder()
                                      .startTimestamp(left.getStartTimestamp())
                                      .endTimestamp(left.getEndTimestamp())
                                      .interval(left.getInterval())
                                      .table(table)
                                      .rows(table.rowCount())
                                      .keyColumns(right.getKeyColumns())
                                      .valColumns(right.getValColumns())
                                      .build();
    }

    private IntermediateQueryResult applyVectorOverScalar(IntermediateQueryResult left, IntermediateQueryResult right) {
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
            Column result = ColumnOperator.VectorOverScalarOperator.apply(col, rColumn, col.getName(), this.getOperatorIndex());
            table.addColumn(result);
        }

        return IntermediateQueryResult.builder()
                                      .startTimestamp(left.getStartTimestamp())
                                      .endTimestamp(left.getEndTimestamp())
                                      .interval(left.getInterval())
                                      .table(table)
                                      .rows(table.rowCount())
                                      .keyColumns(left.getKeyColumns())
                                      .valColumns(left.getValColumns())
                                      .build();
    }

    /**
     * join these two maps by its keyNames
     */
    private IntermediateQueryResult applyVectorOverVector(IntermediateQueryResult left, IntermediateQueryResult right) {
        if (!left.getKeyColumns().equals(right.getKeyColumns())) {
            return IntermediateQueryResult.builder()
                                          // create an EMPTY table
                                          .table(new ColumnarTable())
                                          .build();
        }

        // Join ALL columns together
        List<Column> columns = HashJoiner.join(left.getTable(),
                                               right.getTable(),
                                               left.getKeyColumns(),
                                               left.getValColumns().stream().map((col) -> left.getTable().getColumn(col)).toList(),
                                               right.getValColumns().stream().map((col) -> right.getTable().getColumn(col)).toList());

        // Apply operator on the first column of each table
        int leftColIndex = left.getKeyColumns().size();
        int rightColIndex = leftColIndex + left.getValColumns().size();
        Column result = ColumnOperator.VectorOverVectorOperator.apply(columns.get(leftColIndex),
                                                                      columns.get(rightColIndex),
                                                                      this.resultColumnName,
                                                                      this.getOperatorIndex());

        ColumnarTable table = new ColumnarTable();
        List<String> valueColumns = new ArrayList<>();

        //
        // Key columns
        //
        int i = 0;
        for (; i < left.getKeyColumns().size(); i++) {
            table.addColumn(columns.get(i));
        }

        //
        // Extra Value columns
        //
        valueColumns.add(table.addColumn(result).getName());
        for (
            // Skip the first value column on the left column
            i++;
            i < left.getValColumns().size();
            i++) {
            valueColumns.add(table.addColumn(columns.get(i)).getName());
        }
        for (
            // Skip the first value column on the right column
            i++;
            i < right.getValColumns().size();
            i++) {
            valueColumns.add(table.addColumn(columns.get(i)).getName());
        }

        //
        // User defined retained columns
        //
        if (!this.retainedColumns.isEmpty()) {

            // Use to check if the column we want to add has already been added
            Set<String> resultColumns = new HashSet<>();
            resultColumns.addAll(left.getKeyColumns());
            resultColumns.addAll(valueColumns);

            for (String retainedColumnName : retainedColumns) {
                if (resultColumns.contains(retainedColumnName)) {
                    // The column has already been in the result set
                    continue;
                }

                Column col = columns.stream()
                                    .filter((c) -> c.getName().equals(retainedColumnName))
                                    .findFirst()
                                    .orElse(null);
                if (col != null) {
                    table.addColumn(col);
                    valueColumns.add(col.getName());
                }
            }
        }

        return IntermediateQueryResult.builder()
                                      .interval(left.getInterval())
                                      .startTimestamp(left.getStartTimestamp())
                                      .endTimestamp(left.getEndTimestamp())
                                      .table(table)
                                      .rows(result.size())
                                      .keyColumns(left.getKeyColumns())
                                      .valColumns(valueColumns)
                                      .build();
    }
}
