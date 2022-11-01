package org.bithon.server.storage.datasource.aggregator;/*
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

import org.bithon.server.storage.datasource.spec.IMetricSpec;
import org.bithon.server.storage.datasource.spec.PostAggregatorExpressionVisitor;
import org.bithon.server.storage.datasource.spec.PostAggregatorMetricSpec;
import org.junit.Assert;
import org.junit.Test;

public class PostAggregatorExpressionTest {

    @Test
    public void testVariableExpression() {
        PostAggregatorMetricSpec metricSpec = new PostAggregatorMetricSpec("avg",
                                                                           "dis",
                                                                           "",
                                                                           "1000/{interval}",
                                                                           "long",
                                                                           true);

        final StringBuilder sb = new StringBuilder();
        metricSpec.visitExpression(new PostAggregatorExpressionVisitor() {
            @Override
            public void visitMetric(IMetricSpec metricSpec) {
            }

            @Override
            public void visitConstant(String number) {
                sb.append(number);
            }

            @Override
            public void visitorOperator(String operator) {
                sb.append(operator);
            }

            @Override
            public void beginSubExpression() {
            }

            @Override
            public void endSubExpression() {
            }

            @Override
            public void visitVariable(String variable) {
                sb.append(10);
            }
        });
        Assert.assertEquals("1000/10", sb.toString());
    }

    @Test
    public void testFunctionExpression() {
        String functionExpression = "round(100)";
        PostAggregatorMetricSpec metricSpec = new PostAggregatorMetricSpec("avg",
                                                                           "dis",
                                                                           "",
                                                                           functionExpression,
                                                                           "long",
                                                                           true);

        final StringBuilder sb = new StringBuilder();
        metricSpec.visitExpression(new PostAggregatorExpressionVisitor() {
            @Override
            public void visitConstant(String number) {
                sb.append(number);
            }

            @Override
            public void visitVariable(String variable) {
                sb.append(10);
            }

            @Override
            public void beginFunction(String name) {
                sb.append(name);
                sb.append('(');
            }

            @Override
            public void endFunctionArgument(int argIndex, int argCount) {
                sb.append(')');
            }
        });
        Assert.assertEquals(functionExpression, sb.toString());
    }
}
