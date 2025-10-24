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

package org.bithon.server.datasource.reader.clickhouse.function;


import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.expression.serialization.IdentifierQuotaStrategy;
import org.bithon.server.datasource.DefaultSchema;
import org.bithon.server.datasource.TimestampSpec;
import org.bithon.server.datasource.column.LongColumn;
import org.bithon.server.datasource.column.ObjectColumn;
import org.bithon.server.datasource.column.StringColumn;
import org.bithon.server.datasource.expression.ExpressionASTBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 25/10/25 5:34 pm
 */
public class HasFunctionTest {
    private final DefaultSchema schema = new DefaultSchema(
        "schema",
        "schema",
        new TimestampSpec("timestamp"),
        List.of(
            new StringColumn("a", "a"),
            new LongColumn("intB", "intB"),
            new ObjectColumn("objC", "objC")
        ),
        Collections.emptyList()
    );

    @BeforeAll
    static void initialize() {
        new ClickHouseFunctionRegistry().afterPropertiesSet();
    }

    @Test
    public void test_HasFunction_OnObjectColumn_OK() {
        IExpression expr = ExpressionASTBuilder.builder()
                                               .schema(schema)
                                               .functions(Functions.getInstance())
                                               .build("has(objC, 'a')");
        Assertions.assertEquals("has(\"objC\", 'a')", expr.serializeToText(IdentifierQuotaStrategy.DOUBLE_QUOTE));
    }

    @Test
    public void test_HasFunction_NoStringArg_Fail() {
        Assertions.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.builder()
                                                                                            .schema(schema)
                                                                                            .functions(Functions.getInstance())
                                                                                            .build("has(objC, 1)"));
    }

    @Test
    public void test_HasFunction_OnIntegerColumn_Fail() {
        Assertions.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.builder()
                                                                                            .schema(schema)
                                                                                            .functions(Functions.getInstance())
                                                                                            .build("has(intB, 'a')"));
    }

    @Test
    public void test_HasFunction_OnStringColumn_Fail() {
        Assertions.assertThrows(InvalidExpressionException.class, () -> ExpressionASTBuilder.builder()
                                                                                            .schema(schema)
                                                                                            .functions(Functions.getInstance())
                                                                                            .build("has(a, 'a')"));
    }
}
