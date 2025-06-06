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

package org.bithon.server.datasource.reader.postgresql;

import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.expression.serialization.IdentifierQuotaStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author Frank Chen
 * @date 20/1/24 9:03 pm
 */
public class PostgreSqlDialectTest {

    @Test
    public void testTransformStartsWith() {
        IExpression expr = new PostgreSqlDialect().transform(new FunctionExpression(Functions.getInstance().getFunction("startsWith"),
                                                                                    new IdentifierExpression("a"),
                                                                                    LiteralExpression.ofString("1231")));
        Assertions.assertEquals("a like '1231%'", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testTransformEndsWith() {
        IExpression expr = new PostgreSqlDialect().transform(new FunctionExpression(Functions.getInstance().getFunction("endsWith"),
                                                                                    new IdentifierExpression("a"),
                                                                                    LiteralExpression.ofString("1231")));
        Assertions.assertEquals("a like '%1231'", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void testHasToken() {
        IExpression expr = new PostgreSqlDialect().transform(new FunctionExpression(Functions.getInstance().getFunction("hasToken"),
                                                                                    new IdentifierExpression("a"),
                                                                                    LiteralExpression.ofString("1231")));
        Assertions.assertEquals("a like '%1231%'", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_SafeDivision() {
        IExpression expr = new PostgreSqlDialect().transform(
            new ArithmeticExpression.DIV(
                new IdentifierExpression("a"),
                new IdentifierExpression("b")
            )
        );
        Assertions.assertEquals("CASE WHEN ( b <> 0 ) THEN ( a / b ) ELSE ( 0 ) END", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_SafeDivision_2() {
        IExpression expr = new PostgreSqlDialect().transform(
            new ArithmeticExpression.DIV(
                new FunctionExpression(Functions.getInstance().getFunction("sum"), new IdentifierExpression("a").setDataType(IDataType.LONG)),
                new FunctionExpression(Functions.getInstance().getFunction("sum"), new IdentifierExpression("a").setDataType(IDataType.LONG))
            )
        );
        Assertions.assertEquals("CASE WHEN ( sum(a) <> 0 ) THEN ( sum(a) / sum(a) ) ELSE ( 0 ) END", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }
}
