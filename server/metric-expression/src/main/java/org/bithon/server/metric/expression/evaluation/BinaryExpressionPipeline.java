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


import org.bithon.server.web.service.datasource.api.ColumnarResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author frank.chen021@outlook.com
 * @date 4/4/25 3:49 pm
 */
public abstract class BinaryExpressionPipeline implements IPipeline {
    private final IPipeline lhs;
    private final IPipeline rhs;

    public static class Add extends BinaryExpressionPipeline {
        public Add(IPipeline left, IPipeline right) {
            super(left, right);
        }

        @Override
        double apply(double l, double r) {
            return l + r;
        }
    }

    public static class Sub extends BinaryExpressionPipeline {
        public Sub(IPipeline left, IPipeline right) {
            super(left, right);
        }

        @Override
        double apply(double l, double r) {
            return l - r;
        }
    }

    public static class Mul extends BinaryExpressionPipeline {
        public Mul(IPipeline left, IPipeline right) {
            super(left, right);
        }

        @Override
        double apply(double l, double r) {
            return l * r;
        }
    }

    public static class Div extends BinaryExpressionPipeline {
        public Div(IPipeline left, IPipeline right) {
            super(left, right);
        }

        @Override
        double apply(double l, double r) {
            return l / r;
        }
    }

    protected BinaryExpressionPipeline(IPipeline left, IPipeline right) {
        this.lhs = left;
        this.rhs = right;
    }

    @Override
    public CompletableFuture<ColumnarResponse> execute() {
        CompletableFuture<ColumnarResponse> leftFuture = this.lhs.execute();
        CompletableFuture<ColumnarResponse> rightFuture = this.rhs.execute();

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
        String lValueName = left.getValues().get(0);
        List<Object> lValues = left.getColumns().get(lValueName);
        double lValue = ((Number) lValues.get(0)).doubleValue();

        String rValueName = right.getValues().get(0);
        List<Object> rValues = right.getColumns().get(rValueName);
        double rValue = ((Number) rValues.get(0)).doubleValue();

        double result = apply(lValue, rValue);
        lValues.set(0, result);

        return left;
    }

    private ColumnarResponse applyScalarOverVector(ColumnarResponse left, ColumnarResponse right, boolean sign) {
        String lValueName = left.getValues().get(0);
        List<Object> lValues = left.getColumns().get(lValueName);
        double lValue = ((Number) lValues.get(0)).doubleValue();

        String rValueName = right.getValues().get(0);
        List<Object> rValues = right.getColumns().get(rValueName);
        for (int i = 0, size = rValues.size(); i < size; i++) {
            double rValue = ((Number) rValues.get(i)).doubleValue();

            double v = sign ? apply(lValue, rValue) : apply(rValue, lValue);
            rValues.set(i, v);
        }

        return right;
    }

    private ColumnarResponse applyVectorOverVector(ColumnarResponse left, ColumnarResponse right) {
        // Implement the logic for scalar and vector operation
        /*
                Map<Map<String, Object>, Double> leftMap = toKeyValueMap(left);
        Map<Map<String, Object>, Double> rightMap = toKeyValueMap(right);
        Set<Map<String, Object>> allKeys = new HashSet<>(leftMap.keySet());
        allKeys.retainAll(rightMap.keySet()); // strict PromQL behavior: only operate on intersected label sets

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> key : allKeys) {
            double lv = leftMap.getOrDefault(key, 0.0);
            double rv = rightMap.getOrDefault(key, 0.0);
            double computed = operator.apply(lv, rv);

            Map<String, Object> row = new HashMap<>(key);
            row.put("value", computed);
            result.add(row);
        }
        return result;*/
        return null;
    }
}
