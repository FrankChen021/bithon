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

package org.bithon.server.datasource.reader.clickhouse.expression;

import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.expression.serialization.IdentifierQuotaStrategy;
import org.bithon.server.datasource.DefaultSchema;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.TimestampSpec;
import org.bithon.server.datasource.column.StringColumn;
import org.bithon.server.datasource.column.aggregatable.sum.AggregateLongSumColumn;
import org.bithon.server.datasource.expression.ExpressionASTBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;


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

        Assertions.assertEquals("a like '%-ab-%'", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testHasTokenFunctionOptimizer_ReplaceToLIKEDirectly_Escaped() {
        IExpression expr = ExpressionASTBuilder.builder()
                                               .functions(Functions.getInstance())
                                               .build("hasToken(a, '_ab_')")
                                               .accept(new ClickHouseExpressionOptimizer());

        Assertions.assertEquals("a like '%\\_ab\\_%'", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testHasTokenFunctionOptimizer_Replaced() {
        IExpression expr = ExpressionASTBuilder.builder()
                                               .functions(Functions.getInstance())
                                               .build("hasToken(a, 'SERVER-ERROR')")
                                               .accept(new ClickHouseExpressionOptimizer());

        Assertions.assertEquals("hasToken(a, 'SERVER') AND hasToken(a, 'ERROR') AND (a like '%SERVER-ERROR%')",
                                expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testHasTokenFunctionOptimizer_Replaced_TokeSeparatorAhead() {
        IExpression expr = ExpressionASTBuilder.builder()
                                               .functions(Functions.getInstance())
                                               .build("hasToken(a, '_SERVER')")
                                               .accept(new ClickHouseExpressionOptimizer());

        Assertions.assertEquals("hasToken(a, 'SERVER') AND (a like '%\\_SERVER%')", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testHasTokenFunctionOptimizer_Replaced_TokeSeparatorAfter() {
        IExpression expr = ExpressionASTBuilder.builder()
                                               .functions(Functions.getInstance())
                                               .build("hasToken(a, 'ERROR_')")
                                               .accept(new ClickHouseExpressionOptimizer());

        Assertions.assertEquals("hasToken(a, 'ERROR') AND (a like '%ERROR\\_%')", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testHasTokenFunctionOptimizer_Replaced_In_CompoundExpression() {
        IExpression expr = ExpressionASTBuilder.builder()
                                               .functions(Functions.getInstance())
                                               .build("hasToken(a, 'SERVER_ERROR') AND hasToken(a, 'EXCEPTION_CODE')")
                                               .accept(new ClickHouseExpressionOptimizer());

        Assertions.assertEquals(
            "hasToken(a, 'SERVER') AND hasToken(a, 'ERROR') AND (a like '%SERVER\\_ERROR%') AND hasToken(a, 'EXCEPTION') AND hasToken(a, 'CODE') AND (a like '%EXCEPTION\\_CODE%')",
            expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_ZeroCountAggregateFunction_OptimizationDoNothing() {
        ISchema schema = new DefaultSchema("test-metrics",
                                           "test-metrics",
                                           new TimestampSpec("timestamp"),
                                           Arrays.asList(new StringColumn("appName", "appName"), new StringColumn("type", "type")),
                                           Collections.singletonList(new AggregateLongSumColumn("metric", "metric")));

        IExpression expr = ExpressionASTBuilder.builder()
                                               .functions(Functions.getInstance())
                                               .build("count()")
                                               .accept(new ClickHouseExpressionOptimizer(schema, null));

        Assertions.assertEquals(
            "count()",
            expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }
}
