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

package org.bithon.server.metric.expression.ast;


import org.bithon.component.commons.expression.IExpression;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 6/4/25 2:58 pm
 */
public class MetricExpressionOptimizerTest {

    @Test
    public void test_Optimize_ConstantFolding() {
        String expression = "1 + 2 + 3";
        IExpression ast = MetricExpressionASTBuilder.parse(expression);
        Assert.assertEquals("(1 + 2) + 3", ast.serializeToText());

        ast = MetricExpressionOptimizer.optimize(ast);
        Assert.assertEquals("6", ast.serializeToText());
    }

    @Test
    public void test_Optimize_ConstantFolding_Mul() {
        String expression = "1 + 2 * 3";
        IExpression ast = MetricExpressionASTBuilder.parse(expression);
        Assert.assertEquals("1 + (2 * 3)", ast.serializeToText());

        ast = MetricExpressionOptimizer.optimize(ast);
        Assert.assertEquals("7", ast.serializeToText());
    }

    @Test
    public void test_Optimize_ConstantFolding_2() {
        String expression = "1 + sum(dataSource.metric) + 2 + 3 + 4";
        IExpression ast = MetricExpressionASTBuilder.parse(expression);
        Assert.assertEquals("(((1 + sum(dataSource.metric)) + 2) + 3) + 4", ast.serializeToText());

        ast = MetricExpressionOptimizer.optimize(ast);
        Assert.assertEquals("sum(dataSource.metric) + 10", ast.serializeToText());
    }
}
