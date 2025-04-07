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


import org.bithon.component.commons.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author frank.chen021@outlook.com
 * @date 4/4/25 3:49 pm
 */
public abstract class BinaryExpressionEvaluator implements IEvaluator {
    private final IEvaluator lhs;
    private final IEvaluator rhs;

    /**
     * Nullable, if NULL, then the name of {@link #lhs} will be used
     */
    private final String resultColumnName;
    private final Set<String> sourceColumns;

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
    }

    protected BinaryExpressionEvaluator(IEvaluator left,
                                        IEvaluator right,
                                        String resultColumnName,
                                        String... sourceColumns) {
        this.lhs = left;
        this.rhs = right;
        this.resultColumnName = resultColumnName == null ? "value" : resultColumnName;
        this.sourceColumns = sourceColumns == null ? Collections.emptySet() : new HashSet<>(Arrays.asList(sourceColumns));
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
                                        EvaluationResult result = null;
                                        if (lhs.isScalar()) {
                                            if (rhs.isScalar()) {
                                                result = applyScalarOverScalar(l, r);
                                            } else {
                                                result = applyScalarOverVector(l, r, true);
                                            }
                                        } else {
                                            if (rhs.isScalar()) {
                                                result = applyScalarOverVector(r, l, false);
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

    private EvaluationResult transformResult(EvaluationResult result, EvaluationResult lResponse, EvaluationResult rResponse) {
        return result;
    }

    private EvaluationResult applyScalarOverScalar(EvaluationResult left,
                                                   EvaluationResult right) {
        String lValueName = left.getValueNames()[0];
        List<Object> lValues = left.getValues().get(lValueName);
        double lValue = ((Number) lValues.get(0)).doubleValue();

        String rValueName = right.getValueNames()[0];
        List<Object> rValues = right.getValues().get(rValueName);
        double rValue = ((Number) rValues.get(0)).doubleValue();

        double result = apply(lValue, rValue);
        lValues.set(0, result);

        return left;
    }

    private EvaluationResult applyScalarOverVector(EvaluationResult left, EvaluationResult right, boolean sign) {
        String lValueName = left.getValueNames()[0];
        List<Object> lValues = left.getValues().get(lValueName);
        double lValue = ((Number) lValues.get(0)).doubleValue();

        String rValueName = right.getValueNames()[0];
        List<Object> rValues = right.getValues().get(rValueName);

        for (int i = 0, size = rValues.size(); i < size; i++) {
            double rValue = ((Number) rValues.get(i)).doubleValue();

            double v = sign ? apply(lValue, rValue) : apply(rValue, lValue);
            rValues.set(i, v);
        }

        return right;
    }

    /**
     * join these two maps by its keyNames
     */
    private EvaluationResult applyVectorOverVector(EvaluationResult left, EvaluationResult right) {
        if (!CollectionUtils.isArrayEqual(left.getKeyNames(), right.getKeyNames())) {
            return EvaluationResult.builder()
                                   .keyNames()
                                   .keys(Collections.emptyList())
                                   .valueNames()
                                   .values(Collections.emptyMap())
                                   .build();
        }

        // Use LinkedHashMap to maintain insertion order
        // the result set is the first one
        Map<String, List<Object>> result = new LinkedHashMap<>();

        List<List<Object>> keys = new ArrayList<>();
        List<Object> resultColumn = result.computeIfAbsent(this.resultColumnName, k -> new ArrayList<>());

        List<List<Object>> lSourceColumns = null;
        List<List<Object>> lProjectedColumns = null;

        List<List<Object>> rSourceColumns = null;
        List<List<Object>> rProjectedColumns = null;
        if (!this.sourceColumns.isEmpty()) {
            lSourceColumns = new ArrayList<>();
            rSourceColumns = new ArrayList<>();
            lProjectedColumns = new ArrayList<>();
            rProjectedColumns = new ArrayList<>();

            for (int i = 0; i < left.getValueNames().length; i++) {
                String col = left.getValueNames()[i];
                if (this.sourceColumns.contains(col)) {
                    lSourceColumns.add(left.getValues().get(col));
                    lProjectedColumns.add(result.computeIfAbsent(col, k -> new ArrayList<>()));
                }
            }
            for (int i = 0; i < right.getValueNames().length; i++) {
                String col = right.getValueNames()[i];
                if (this.sourceColumns.contains(col)) {
                    rSourceColumns.add(right.getValues().get(col));
                    rProjectedColumns.add(result.computeIfAbsent(col, k -> new ArrayList<>()));
                }
            }
        }

        List<Object> lValueColumn = left.getValues().get(left.getValueNames()[0]);
        List<Object> rValueColumn = right.getValues().get(right.getValueNames()[0]);

        Map<List<Object>, Integer> lmap = toMap(left);
        Map<List<Object>, Integer> rmap = toMap(right);

        for (Map.Entry<List<Object>, Integer> lEntry : lmap.entrySet()) {
            List<Object> rowKey = lEntry.getKey();
            int lRowIndex = lEntry.getValue();

            Integer rRowIndex = rmap.get(rowKey);
            if (rRowIndex != null) {
                double leftValue = ((Number) lValueColumn.get(lRowIndex)).doubleValue();
                double rightValue = ((Number) rValueColumn.get(rRowIndex)).doubleValue();
                double v = apply(leftValue, rightValue);

                keys.add(rowKey);
                resultColumn.add(v);

                if (lSourceColumns != null) {
                    for (int i = 0; i < lSourceColumns.size(); i++) {
                        List<Object> sourceColumn = lSourceColumns.get(i);
                        lProjectedColumns.get(i).add(sourceColumn.get(lRowIndex));
                    }
                }
                if (rSourceColumns != null) {
                    for (int i = 0; i < rSourceColumns.size(); i++) {
                        List<Object> sourceColumn = rSourceColumns.get(i);
                        rProjectedColumns.get(i).add(sourceColumn.get(rRowIndex));
                    }
                }
            }
        }

        // Build response
        return EvaluationResult.builder()
                               .startTimestamp(left.getStartTimestamp())
                               .endTimestamp(left.getEndTimestamp())
                               .interval(left.getInterval())
                               .keyNames(left.getKeyNames())
                               .keys(keys)
                               .valueNames(result.keySet().toArray(new String[0]))
                               .values(result)
                               .build();
    }

    /**
     * @return a Map where key is the keyNames and value is the index
     */
    private Map<List<Object>, Integer> toMap(EvaluationResult response) {
        // Use LinkedHashMap to maintain insertion order
        Map<List<Object>, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < response.getRows(); i++) {
            List<Object> rowKey = response.getKeys().get(i);
            map.put(rowKey, i);
        }
        return map;
    }
}
