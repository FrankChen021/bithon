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

package org.bithon.server.storage.datasource.aggregator;

import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.spec.IMetricSpec;
import org.bithon.server.storage.datasource.spec.PostAggregatorExpressionVisitor;
import org.bithon.server.storage.datasource.spec.PostAggregatorMetricSpec;
import org.bithon.server.storage.datasource.spec.sum.LongSumMetricSpec;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PostAggregatorExpressionTest {

    @Test
    public void testVariableExpression() {
        PostAggregatorMetricSpec metricSpec = new PostAggregatorMetricSpec("avg",
                                                                           "dis",
                                                                           "",
                                                                           "1000/{interval}",
                                                                           "long",
                                                                           true);

        ExpressionGenerator g = new ExpressionGenerator();
        metricSpec.visitExpression(g);
        Assert.assertEquals("1000/{interval}", g.getGenerated());
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

        ExpressionGenerator g = new ExpressionGenerator();
        metricSpec.visitExpression(g);
        Assert.assertEquals(functionExpression, g.getGenerated());
    }

    @Test
    public void testExpressionInFunctionExpression() {
        DataSourceSchema schema = new DataSourceSchema("display", "name", null, Collections.emptyList(),
                                                       Stream.of("a", "b", "c", "d").map((s)->new LongSumMetricSpec(s, s, s, s, true)).collect(Collectors.toList()));
        String functionExpression = "round(a*b/c+d)";
        PostAggregatorMetricSpec metricSpec = new PostAggregatorMetricSpec("avg",
                                                                           "dis",
                                                                           "",
                                                                           functionExpression,
                                                                           "long",
                                                                           true);
        metricSpec.setOwner(schema);

        ExpressionGenerator g = new ExpressionGenerator();
        metricSpec.visitExpression(g);
        Assert.assertEquals(functionExpression, g.getGenerated());
    }

    @Test
    public void testMoreArgumentInFunctionExpression() {
        String functionExpression = "round(100,99,98)";
        PostAggregatorMetricSpec metricSpec = new PostAggregatorMetricSpec("avg",
                                                                           "dis",
                                                                           "",
                                                                           functionExpression,
                                                                           "long",
                                                                           true);

        ExpressionGenerator g = new ExpressionGenerator();
        metricSpec.visitExpression(g);
        Assert.assertEquals(functionExpression, g.getGenerated());
    }

    private static class ExpressionGenerator implements PostAggregatorExpressionVisitor {
        private final StringBuilder sb;

        public ExpressionGenerator() {
            this.sb = new StringBuilder(64);
        }

        @Override
        public void visitMetric(IMetricSpec metricSpec) {
            sb.append(metricSpec.getName());
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
        public void visitVariable(String variable) {
            sb.append('{');
            sb.append(variable);
            sb.append('}');
        }

        @Override
        public void beginFunction(String name) {
            sb.append(name);
            sb.append('(');
        }

        @Override
        public void endFunction() {
            sb.append(')');
        }

        @Override
        public void endFunctionArgument(int argIndex, boolean isLast) {
            if (!isLast) {
                sb.append(',');
            }
        }

        public String getGenerated() {
            return sb.toString();
        }
    }
}
