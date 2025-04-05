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
import org.bithon.server.web.service.datasource.api.ColumnarResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
            super(left, right);
        }

        @Override
        double apply(double l, double r) {
            return l + r;
        }
    }

    public static class Sub extends BinaryExpressionEvaluator {
        public Sub(IEvaluator left, IEvaluator right) {
            super(left, right);
        }

        @Override
        double apply(double l, double r) {
            return l - r;
        }
    }

    public static class Mul extends BinaryExpressionEvaluator {
        public Mul(IEvaluator left, IEvaluator right) {
            super(left, right);
        }

        @Override
        double apply(double l, double r) {
            return l * r;
        }
    }

    public static class Div extends BinaryExpressionEvaluator {
        public Div(IEvaluator left, IEvaluator right) {
            super(left, right);
        }

        @Override
        double apply(double l, double r) {
            return l / r;
        }
    }

    protected BinaryExpressionEvaluator(IEvaluator left, IEvaluator right) {
        this.lhs = left;
        this.rhs = right;
    }

    @Override
    public CompletableFuture<ColumnarResponse> evaluate() {
        CompletableFuture<ColumnarResponse> leftFuture = this.lhs.evaluate();
        CompletableFuture<ColumnarResponse> rightFuture = this.rhs.evaluate();

        return CompletableFuture.allOf(leftFuture, rightFuture)
                                .thenApply(v -> {
                                    try {
                                        ColumnarResponse l = leftFuture.get();
                                        ColumnarResponse r = rightFuture.get();
                                        if (lhs.isScalar()) {
                                            if (rhs.isScalar()) {
                                                return applyScalarOverScalar(l, r);
                                            } else {
                                                return applyScalarOverVector(l, r, true);
                                            }
                                        } else {
                                            if (rhs.isScalar()) {
                                                return applyScalarOverVector(r, l, false);
                                            } else {
                                                return applyVectorOverVector(l, r);
                                            }
                                        }
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

    private ColumnarResponse applyScalarOverScalar(ColumnarResponse left, ColumnarResponse right) {
        String lValueName = left.getValues()[0];
        List<Object> lValues = left.getColumns().get(lValueName);
        double lValue = ((Number) lValues.get(0)).doubleValue();

        String rValueName = right.getValues()[0];
        List<Object> rValues = right.getColumns().get(rValueName);
        double rValue = ((Number) rValues.get(0)).doubleValue();

        double result = apply(lValue, rValue);
        lValues.set(0, result);

        return left;
    }

    private ColumnarResponse applyScalarOverVector(ColumnarResponse left, ColumnarResponse right, boolean sign) {
        String lValueName = left.getValues()[0];
        List<Object> lValues = left.getColumns().get(lValueName);
        double lValue = ((Number) lValues.get(0)).doubleValue();

        String rValueName = right.getValues()[0];
        List<Object> rValues = right.getColumns().get(rValueName);

        for (int i = 0, size = rValues.size(); i < size; i++) {
            double rValue = ((Number) rValues.get(i)).doubleValue();

            double v = sign ? apply(lValue, rValue) : apply(rValue, lValue);
            rValues.set(i, v);
        }

        return right;
    }

    private ColumnarResponse applyVectorOverVector(ColumnarResponse left, ColumnarResponse right) {
        if (left.getKeys().length != right.getKeys().length) {
            return ColumnarResponse.builder()
                                   .keys(new String[0])
                                   .values(new String[0])
                                   .columns(Collections.emptyMap())
                                   .build();
        }
        if (!CollectionUtils.isArrayEqual(left.getKeys(), right.getKeys())) {
            return ColumnarResponse.builder()
                                   .keys(new String[0])
                                   .values(new String[0])
                                   .columns(Collections.emptyMap())
                                   .build();
        }

        Map<List<Object>, Object> lmap = toMap(left);
        Map<List<Object>, Object> rmap = toMap(right);

        //
        // join these two maps by its keys
        //
        // Create result structure
        Map<String, List<Object>> resultColumns = new HashMap<>();
        String[] keys = left.getKeys();

        String valueColumn = "value";

        // Initialize result columns
        for (String key : keys) {
            resultColumns.put(key, new ArrayList<>());
        }
        resultColumns.put(valueColumn, new ArrayList<>());

        // Join maps by keys and compute results
        for (Map.Entry<List<Object>, Object> entry : lmap.entrySet()) {
            List<Object> keyValues = entry.getKey();
            if (rmap.containsKey(keyValues)) {
                // Add dimension keys to result
                for (int i = 0; i < keys.length; i++) {
                    resultColumns.get(keys[i]).add(keyValues.get(i));
                }

                // Calculate and add value
                double leftValue = ((Number) entry.getValue()).doubleValue();
                double rightValue = ((Number) rmap.get(keyValues)).doubleValue();
                double result = apply(leftValue, rightValue);
                resultColumns.get(valueColumn).add(result);
            }
        }

        // Build response
        return ColumnarResponse.builder()
                               .keys(keys)
                               .values("value")
                               .columns(resultColumns)
                               .build();
    }

    private Map<List<Object>, Object> toMap(ColumnarResponse response) {
        // Use LinkedHashMap to maintain insertion order
        Map<List<Object>, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < response.getRows(); i++) {

            List<Object> rowKey = new ArrayList<>(response.getRows());
            for (int j = 0; j < response.getKeys().length; j++) {
                String key = response.getKeys()[j];
                Object value = response.getColumns().get(key).get(i);
                rowKey.add(value);
            }

            String valName = response.getValues()[0];
            map.put(rowKey, response.getColumns().get(valName).get(i));
        }
        return map;
    }
}
