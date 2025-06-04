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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author frank.chen021@outlook.com
 * @date 6/4/25 2:58 pm
 */
public class MetricExpressionOptimizerTest {

    @Test
    public void test_Optimize_ConstantFolding() {
        String expression = "1 + 2 + 3";
        IExpression ast = MetricExpressionASTBuilder.parse(expression);
        Assertions.assertEquals("(1 + 2) + 3", ast.serializeToText());

        ast = MetricExpressionOptimizer.optimize(ast);
        Assertions.assertEquals("6", ast.serializeToText());
    }

    @Test
    public void test_Optimize_ConstantFolding_Mul() {
        String expression = "1 + 2 * 3";
        IExpression ast = MetricExpressionASTBuilder.parse(expression);
        Assertions.assertEquals("1 + (2 * 3)", ast.serializeToText());

        ast = MetricExpressionOptimizer.optimize(ast);
        Assertions.assertEquals("7", ast.serializeToText());
    }

    @Test
    public void test_Optimize_ConstantFolding_2() {
        String expression = "1 + sum(dataSource.metric) + 2 * 3 + 4";
        IExpression ast = MetricExpressionASTBuilder.parse(expression);
        Assertions.assertEquals("((1 + sum(dataSource.metric)) + (2 * 3)) + 4", ast.serializeToText());

        ast = MetricExpressionOptimizer.optimize(ast);
        Assertions.assertEquals("sum(dataSource.metric) + 11", ast.serializeToText());
    }

    @Test
    public void test_Optimize_ConstantFolding_3() {
        String expression = "2 + sum(dataSource.metric) - 2 - 3 + 3";
        IExpression ast = MetricExpressionASTBuilder.parse(expression);

        ast = MetricExpressionOptimizer.optimize(ast);
        Assertions.assertEquals("sum(dataSource.metric)", ast.serializeToText());
    }

    @Test
    public void test_Optimize_ConstantFolding_Percentage() {
        String expression = "sum(dataSource.metric) + 10% + 10%";
        IExpression ast = MetricExpressionASTBuilder.parse(expression);

        ast = MetricExpressionOptimizer.optimize(ast);
        Assertions.assertEquals("sum(dataSource.metric) + 0.2", ast.serializeToText());
    }

    @Test
    public void test_Optimize_ConstantFolding_ReadableNumber() {
        String expression = "sum(dataSource.metric) + 2GiB + 3GiB";
        IExpression ast = MetricExpressionASTBuilder.parse(expression);

        ast = MetricExpressionOptimizer.optimize(ast);
        Assertions.assertEquals("sum(dataSource.metric) + 5368709120", ast.serializeToText());
    }

    @Test
    public void test_Optimize_ConstantFolding_Duration() {
        String expression = "sum(dataSource.metric) + 1d + 1m";
        IExpression ast = MetricExpressionASTBuilder.parse(expression);

        ast = MetricExpressionOptimizer.optimize(ast);
        Assertions.assertEquals("sum(dataSource.metric) + 86460", ast.serializeToText());
    }
}
