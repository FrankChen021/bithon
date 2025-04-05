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

    private ColumnarResponse applyScalarOverVector(ColumnarResponse left, ColumnarResponse right, boolean sign) {
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

    private ColumnarResponse applyVectorOverVector(ColumnarResponse left, ColumnarResponse right) {
        if (!CollectionUtils.isArrayEqual(left.getKeyNames(), right.getKeyNames())) {
            return ColumnarResponse.builder()
                                   .keyNames()
                                   .keys(Collections.emptyList())
                                   .valueNames()
                                   .values(Collections.emptyMap())
                                   .build();
        }

        Map<List<Object>, Object> lmap = toMap(left);
        Map<List<Object>, Object> rmap = toMap(right);

        //
        // join these two maps by its keyNames
        //
        List<List<Object>> keys = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        for (Map.Entry<List<Object>, Object> lEntry : lmap.entrySet()) {
            List<Object> rowKey = lEntry.getKey();

            Object rValue = rmap.get(rowKey);
            if (rValue != null) {
                double leftValue = ((Number) lEntry.getValue()).doubleValue();
                double rightValue = ((Number) rValue).doubleValue();
                double result = apply(leftValue, rightValue);

                keys.add(rowKey);
                values.add(result);
            }
        }

        // Build response
        return ColumnarResponse.builder()
                               .startTimestamp(left.getStartTimestamp())
                               .endTimestamp(left.getEndTimestamp())
                               .interval(left.getInterval())
                               .keyNames(left.getKeyNames())
                               .keys(keys)
                               .valueNames("value")
                               .values(Map.of("value", values))
                               .build();
    }

    private Map<List<Object>, Object> toMap(ColumnarResponse response) {
        // Use LinkedHashMap to maintain insertion order
        Map<List<Object>, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < response.getRows(); i++) {

            List<Object> rowKey = response.getKeys().get(i);

            String valName = response.getValueNames()[0];
            Object val = response.getValues().get(valName).get(i);

            map.put(rowKey, val);
        }
        return map;
    }
}
