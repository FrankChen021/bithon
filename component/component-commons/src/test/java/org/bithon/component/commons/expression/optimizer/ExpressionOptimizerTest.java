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

package org.bithon.component.commons.expression.optimizer;

import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.expression.optimzer.ExpressionOptimizer;
import org.bithon.component.commons.expression.serialization.IdentifierQuotaStrategy;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.HumanReadableNumber;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * NOTE:
 * Most tests are placed in the server-storage module because it provides AST parser for simpler test construction
 *
 * @author frank.chen021@outlook.com
 * @date 5/6/24 10:15 am
 */
public class ExpressionOptimizerTest {

    @Test
    public void testLogicalExpression_FlattenAND() {
        LogicalExpression expr = new LogicalExpression.AND(
            new ComparisonExpression.EQ(new IdentifierExpression("a"), LiteralExpression.ofLong(1)),

            new LogicalExpression.AND(
                new ComparisonExpression.EQ(new IdentifierExpression("b"), LiteralExpression.ofLong(2)),
                new ComparisonExpression.EQ(new IdentifierExpression("c"), LiteralExpression.ofLong(3))
            ),

            new LogicalExpression.AND(
                new ComparisonExpression.EQ(new IdentifierExpression("d"), LiteralExpression.ofLong(4)),
                new ComparisonExpression.EQ(new IdentifierExpression("e"), LiteralExpression.ofLong(5))
            )
        );

        expr.accept(new ExpressionOptimizer.AbstractOptimizer());

        Assertions.assertEquals(5, expr.getOperands().size());
        Assertions.assertEquals("(a = 1) AND (b = 2) AND (c = 3) AND (d = 4) AND (e = 5)", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testLogicalExpression_FlattenOR() {
        LogicalExpression expr = new LogicalExpression.OR(
            new ComparisonExpression.EQ(new IdentifierExpression("a"), LiteralExpression.ofLong(1)),

            new LogicalExpression.OR(
                new ComparisonExpression.EQ(new IdentifierExpression("b"), LiteralExpression.ofLong(2)),
                new ComparisonExpression.EQ(new IdentifierExpression("c"), LiteralExpression.ofLong(3))
            )
        );

        expr.accept(new ExpressionOptimizer.AbstractOptimizer());

        Assertions.assertEquals(3, expr.getOperands().size());
        Assertions.assertEquals("(a = 1) OR (b = 2) OR (c = 3)", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testLogicalExpression_NoFlatten() {
        LogicalExpression expr = new LogicalExpression.AND(
            new ComparisonExpression.EQ(new IdentifierExpression("a"), LiteralExpression.ofLong(1)),

            new LogicalExpression.OR(
                new ComparisonExpression.EQ(new IdentifierExpression("b"), LiteralExpression.ofLong(2)),
                new ComparisonExpression.EQ(new IdentifierExpression("c"), LiteralExpression.ofLong(3))
            )
        );

        expr.accept(new ExpressionOptimizer.AbstractOptimizer());

        Assertions.assertEquals(2, expr.getOperands().size());
        Assertions.assertEquals("(a = 1) AND ((b = 2) OR (c = 3))", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testLogicalExpression_FlattenRecursively() {
        LogicalExpression expr = new LogicalExpression.AND(
            new ComparisonExpression.EQ(new IdentifierExpression("a"), LiteralExpression.ofLong(1)),

            new LogicalExpression.AND(
                new ComparisonExpression.EQ(new IdentifierExpression("b"), LiteralExpression.ofLong(2)),

                new LogicalExpression.AND(
                    new ComparisonExpression.EQ(new IdentifierExpression("c"), LiteralExpression.ofLong(3)),

                    new LogicalExpression.AND(
                        new ComparisonExpression.EQ(new IdentifierExpression("d"), LiteralExpression.ofLong(4))
                    )
                )
            )
        );

        expr.accept(new ExpressionOptimizer.AbstractOptimizer());

        Assertions.assertEquals(4, expr.getOperands().size());
        Assertions.assertEquals("(a = 1) AND (b = 2) AND (c = 3) AND (d = 4)", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testLogicalExpression_FlattenNOT() {
        IExpression expr = new LogicalExpression.NOT(
            new ComparisonExpression.EQ(new IdentifierExpression("a"), LiteralExpression.ofLong(1)),

            new LogicalExpression.AND(
                new ComparisonExpression.EQ(new IdentifierExpression("b"), LiteralExpression.ofLong(2)),

                new LogicalExpression.AND(
                    new ComparisonExpression.EQ(new IdentifierExpression("c"), LiteralExpression.ofLong(3)),

                    new LogicalExpression.AND(
                        new ComparisonExpression.EQ(new IdentifierExpression("d"), LiteralExpression.ofLong(4))
                    )
                )
            )
        );

        expr = ExpressionOptimizer.optimize(expr);
        Assertions.assertEquals("NOT ((a = 1) AND (b = 2) AND (c = 3) AND (d = 4))", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testLogicalExpression_FlattenNOT_To_One() {
        IExpression expr = new LogicalExpression.NOT(
            new ComparisonExpression.EQ(LiteralExpression.ofLong(1), LiteralExpression.ofLong(1))
        );

        expr = ExpressionOptimizer.optimize(expr);
        Assertions.assertEquals("false", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testConstantFolding_DoNothing() {
        IExpression ast = LiteralExpression.ofLong(1);
        ast = ExpressionOptimizer.optimize(ast);
        Assertions.assertEquals("1", ast.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_ConstantFolding_Add() {
        IExpression ast = new ArithmeticExpression.ADD(
            LiteralExpression.of(3),
            LiteralExpression.ofLong(2)
        );

        ast = ExpressionOptimizer.optimize(ast);
        Assertions.assertEquals("5", ast.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_ConstantFolding_FoldingList() {
        IExpression ast = new ArithmeticExpression.ADD(
            new ArithmeticExpression.ADD(
                LiteralExpression.ofLong(1),
                LiteralExpression.ofLong(2)
            ),
            LiteralExpression.ofLong(3)
        );

        ast = ExpressionOptimizer.optimize(ast);
        Assertions.assertEquals("6", ast.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_ConstantFolding_FoldingList_2() {
        IExpression ast = new ArithmeticExpression.ADD(
            new ArithmeticExpression.SUB(
                LiteralExpression.ofLong(1),
                LiteralExpression.ofLong(2)
            ),
            LiteralExpression.ofLong(3)
        );

        ast = ExpressionOptimizer.optimize(ast);
        Assertions.assertEquals("2", ast.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    /**
     * (lower(a) + 2) + 2 -> lower(a) + 5
     */
    @Test
    public void testConstantFolding_AssociativeFolding_Add() {
        IExpression ast = new ArithmeticExpression.ADD(
            new ArithmeticExpression.ADD(
                new FunctionExpression(Functions.getInstance().getFunction("max"), new IdentifierExpression("a").setDataType(IDataType.LONG)),
                LiteralExpression.ofLong(1)
            ),
            LiteralExpression.ofLong(2)
        );
        ast = ExpressionOptimizer.optimize(ast);
        Assertions.assertEquals("max(a) + 3", ast.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    /**
     * (max(a) + 1) + (min(a) + 3) -> (max(a) + min(a)) + 4
     */
    @Test
    public void testConstantFolding_AssociativeFolding_Add_2() {
        IExpression ast = new ArithmeticExpression.ADD(
            new ArithmeticExpression.ADD(
                new FunctionExpression(Functions.getInstance().getFunction("max"), new IdentifierExpression("a").setDataType(IDataType.LONG)),
                LiteralExpression.ofLong(1)
            ),
            new ArithmeticExpression.ADD(
                new FunctionExpression(Functions.getInstance().getFunction("min"), new IdentifierExpression("a").setDataType(IDataType.LONG)),
                LiteralExpression.ofLong(3)
            )
        );
        ast = ExpressionOptimizer.optimize(ast);
        Assertions.assertEquals("(max(a) + min(a)) + 4", ast.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    /**
     * (max(a) * 2) * 2 -> lower(a) * 4
     */
    @Test
    public void testConstantFolding_AssociativeFolding_Mul() {
        IExpression ast = new ArithmeticExpression.MUL(
            new ArithmeticExpression.MUL(
                new FunctionExpression(Functions.getInstance().getFunction("max"), new IdentifierExpression("a").setDataType(IDataType.LONG)),
                LiteralExpression.ofLong(2)
            ),
            LiteralExpression.ofLong(2)
        );
        ast = ExpressionOptimizer.optimize(ast);
        Assertions.assertEquals("max(a) * 4", ast.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    /**
     * (max(a) * 2) * (min(a) * 2) -> (max(a) * min(a)) * 4
     */
    @Test
    public void testConstantFolding_AssociativeFolding_Mul_2() {
        IExpression ast = new ArithmeticExpression.MUL(
            new ArithmeticExpression.MUL(
                new FunctionExpression(Functions.getInstance().getFunction("max"), new IdentifierExpression("a").setDataType(IDataType.LONG)),
                LiteralExpression.ofLong(2)
            ),
            new ArithmeticExpression.MUL(
                new FunctionExpression(Functions.getInstance().getFunction("min"), new IdentifierExpression("a").setDataType(IDataType.LONG)),
                LiteralExpression.ofLong(3)
            )
        );
        ast = ExpressionOptimizer.optimize(ast);
        Assertions.assertEquals("(max(a) * min(a)) * 6", ast.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    /**
     * (max(a) + 2) + (min(a) - 2) -> (max(a) * min(a))
     */
    @Test
    public void testConstantFolding_AssociativeFolding_Hybrid() {
        IExpression ast = new ArithmeticExpression.ADD(
            new ArithmeticExpression.ADD(
                new FunctionExpression(Functions.getInstance().getFunction("max"), new IdentifierExpression("a").setDataType(IDataType.LONG)),
                LiteralExpression.ofLong(2)
            ),
            new ArithmeticExpression.SUB(
                new FunctionExpression(Functions.getInstance().getFunction("min"), new IdentifierExpression("a").setDataType(IDataType.LONG)),
                LiteralExpression.ofLong(2)
            )
        );
        ast = ExpressionOptimizer.optimize(ast);
        Assertions.assertEquals("max(a) + min(a)", ast.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testConstantFolding_ReadableNumber() {
        IExpression ast = new ArithmeticExpression.ADD(
            LiteralExpression.of(HumanReadableNumber.of("1KiB")),
            LiteralExpression.ofLong(2)
        );
        ast = ExpressionOptimizer.optimize(ast);
        Assertions.assertEquals("1026", ast.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testConstantFolding_ReadableDuration() {
        IExpression ast = new ArithmeticExpression.ADD(
            LiteralExpression.of(HumanReadableDuration.parse("1h")),
            LiteralExpression.ofLong(3)
        );
        ast = ExpressionOptimizer.optimize(ast);
        Assertions.assertEquals("3603", ast.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testConstantFolding_ReadablePercentage() {
        IExpression ast = new ArithmeticExpression.ADD(
            LiteralExpression.of(HumanReadablePercentage.of("50%")),
            LiteralExpression.ofLong(3)
        );
        ast = ExpressionOptimizer.optimize(ast);
        Assertions.assertEquals("3.5", ast.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testConstantFolding_In_FunctionArgs() {
        IExpression ast = new FunctionExpression(
            Functions.getInstance().getFunction("substring"),
            LiteralExpression.of("123456789"),

            // index : 3
            new ArithmeticExpression.ADD(
                LiteralExpression.ofLong(1),
                LiteralExpression.ofLong(2)
            ),

            // len: 4
            new ArithmeticExpression.ADD(
                LiteralExpression.ofLong(-1),
                LiteralExpression.ofLong(5)
            )
        );

        ast = ExpressionOptimizer.optimize(ast);
        Assertions.assertEquals("'4567'", ast.serializeToText(IdentifierQuotaStrategy.NONE));
    }
}
