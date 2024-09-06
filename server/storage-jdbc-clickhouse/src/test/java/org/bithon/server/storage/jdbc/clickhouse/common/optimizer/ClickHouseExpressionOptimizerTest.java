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

package org.bithon.server.storage.jdbc.clickhouse.common.optimizer;

import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.server.storage.common.expression.ExpressionASTBuilder;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 2/5/24 10:15pm
 */
public class ClickHouseExpressionOptimizerTest {
    @Test
    public void testHasTokenFunctionOptimizer_ReplaceToLIKEDirectly() {
        IExpression expr = ExpressionASTBuilder.builder()
                                               .functions(Functions.getInstance())
                                               .build("hasToken(a, '-ab-')")
                                               .accept(new ClickHouseExpressionOptimizer());

        Assert.assertEquals("a like '%-ab-%'", expr.serializeToText(null));
    }

    @Test
    public void testHasTokenFunctionOptimizer_ReplaceToLIKEDirectly_Escaped() {
        IExpression expr = ExpressionASTBuilder.builder()
                                               .functions(Functions.getInstance())
                                               .build("hasToken(a, '_ab_')")
                                               .accept(new ClickHouseExpressionOptimizer());

        Assert.assertEquals("a like '%\\_ab\\_%'", expr.serializeToText(null));
    }

    @Test
    public void testHasTokenFunctionOptimizer_Replaced() {
        IExpression expr = ExpressionASTBuilder.builder()
                                               .functions(Functions.getInstance())
                                               .build("hasToken(a, 'SERVER-ERROR')")
                                               .accept(new ClickHouseExpressionOptimizer());

        Assert.assertEquals("(hasToken(a, 'SERVER') AND hasToken(a, 'ERROR') AND a like '%SERVER-ERROR%')",
                            expr.serializeToText(null));
    }

    @Test
    public void testHasTokenFunctionOptimizer_Replaced_TokeSeparatorAhead() {
        IExpression expr = ExpressionASTBuilder.builder()
                                               .functions(Functions.getInstance())
                                               .build("hasToken(a, '_SERVER')")
                                               .accept(new ClickHouseExpressionOptimizer());

        Assert.assertEquals("(hasToken(a, 'SERVER') AND a like '%\\_SERVER%')", expr.serializeToText(null));
    }

    @Test
    public void testHasTokenFunctionOptimizer_Replaced_TokeSeparatorAfter() {
        IExpression expr = ExpressionASTBuilder.builder()
                                               .functions(Functions.getInstance())
                                               .build("hasToken(a, 'ERROR_')")
                                               .accept(new ClickHouseExpressionOptimizer());

        Assert.assertEquals("(hasToken(a, 'ERROR') AND a like '%ERROR\\_%')", expr.serializeToText(null));
    }

    @Test
    public void testHasTokenFunctionOptimizer_Replaced_In_CompoundExpression() {
        IExpression expr = ExpressionASTBuilder.builder()
                                               .functions(Functions.getInstance())
                                               .build("hasToken(a, 'SERVER_ERROR') AND hasToken(a, 'EXCEPTION_CODE')")
                                               .accept(new ClickHouseExpressionOptimizer());

        Assert.assertEquals("(hasToken(a, 'SERVER') AND hasToken(a, 'ERROR') AND a like '%SERVER\\_ERROR%' AND hasToken(a, 'EXCEPTION') AND hasToken(a, 'CODE') AND a like '%EXCEPTION\\_CODE%')", expr.serializeToText(null));
    }
}
