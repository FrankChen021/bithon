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

import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.optimzer.ExpressionOptimizer;
import org.junit.Assert;
import org.junit.Test;

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
            new ComparisonExpression.EQ(new IdentifierExpression("a"), LiteralExpression.create(1)),

            new LogicalExpression.AND(
                new ComparisonExpression.EQ(new IdentifierExpression("b"), LiteralExpression.create(2)),
                new ComparisonExpression.EQ(new IdentifierExpression("c"), LiteralExpression.create(3))
            )
        );

        expr.accept(new ExpressionOptimizer.AbstractOptimizer());

        Assert.assertEquals(3, expr.getOperands().size());
        Assert.assertEquals("(a = 1 AND b = 2 AND c = 3)", expr.serializeToText(null));
    }

    @Test
    public void testLogicalExpression_FlattenOR() {
        LogicalExpression expr = new LogicalExpression.OR(
            new ComparisonExpression.EQ(new IdentifierExpression("a"), LiteralExpression.create(1)),

            new LogicalExpression.OR(
                new ComparisonExpression.EQ(new IdentifierExpression("b"), LiteralExpression.create(2)),
                new ComparisonExpression.EQ(new IdentifierExpression("c"), LiteralExpression.create(3))
            )
        );

        expr.accept(new ExpressionOptimizer.AbstractOptimizer());

        Assert.assertEquals(3, expr.getOperands().size());
        Assert.assertEquals("(a = 1 OR b = 2 OR c = 3)", expr.serializeToText(null));
    }

    @Test
    public void testLogicalExpression_NoFlatten() {
        LogicalExpression expr = new LogicalExpression.AND(
            new ComparisonExpression.EQ(new IdentifierExpression("a"), LiteralExpression.create(1)),

            new LogicalExpression.OR(
                new ComparisonExpression.EQ(new IdentifierExpression("b"), LiteralExpression.create(2)),
                new ComparisonExpression.EQ(new IdentifierExpression("c"), LiteralExpression.create(3))
            )
        );

        expr.accept(new ExpressionOptimizer.AbstractOptimizer());

        Assert.assertEquals(2, expr.getOperands().size());
        Assert.assertEquals("(a = 1 AND (b = 2 OR c = 3))", expr.serializeToText(null));
    }

    @Test
    public void testLogicalExpression_FlattenRecursively() {
        LogicalExpression expr = new LogicalExpression.AND(
            new ComparisonExpression.EQ(new IdentifierExpression("a"), LiteralExpression.create(1)),

            new LogicalExpression.AND(
                new ComparisonExpression.EQ(new IdentifierExpression("b"), LiteralExpression.create(2)),

                new LogicalExpression.AND(
                    new ComparisonExpression.EQ(new IdentifierExpression("c"), LiteralExpression.create(3)),

                    new LogicalExpression.AND(
                        new ComparisonExpression.EQ(new IdentifierExpression("d"), LiteralExpression.create(4))
                        )
                )
            )
        );

        expr.accept(new ExpressionOptimizer.AbstractOptimizer());

        Assert.assertEquals(4, expr.getOperands().size());
        Assert.assertEquals("(a = 1 AND b = 2 AND c = 3 AND d = 4)", expr.serializeToText(null));
    }

    @Test
    public void testLogicalExpression_FlattenNOT() {
        LogicalExpression expr = new LogicalExpression.NOT(
            new ComparisonExpression.EQ(new IdentifierExpression("a"), LiteralExpression.create(1)),

            new LogicalExpression.AND(
                new ComparisonExpression.EQ(new IdentifierExpression("b"), LiteralExpression.create(2)),

                new LogicalExpression.AND(
                    new ComparisonExpression.EQ(new IdentifierExpression("c"), LiteralExpression.create(3)),

                    new LogicalExpression.AND(
                        new ComparisonExpression.EQ(new IdentifierExpression("d"), LiteralExpression.create(4))
                    )
                )
            )
        );

        expr.accept(new ExpressionOptimizer.AbstractOptimizer());

        Assert.assertEquals(4, expr.getOperands().size());
        Assert.assertEquals("NOT (a = 1 AND b = 2 AND c = 3 AND d = 4)", expr.serializeToText(null));
    }
}
