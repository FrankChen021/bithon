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

package org.bithon.server.storage.jdbc.mysql;


import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.expression.serialization.IdentifierQuotaStrategy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 14/4/25 10:18 pm
 */
public class MySQLDialectTest {
    @Test
    public void test_SafeDivision() {
        IExpression expr = new MySQLSqlDialect().transform(
            new ArithmeticExpression.DIV(
                new IdentifierExpression("a"),
                new IdentifierExpression("b")
            )
        );
        Assertions.assertEquals("IF( b <> 0, a / b, 0)", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_SafeDivision_2() {
        IExpression expr = new MySQLSqlDialect().transform(
            new ArithmeticExpression.DIV(
                new FunctionExpression(Functions.getInstance().getFunction("sum"), new IdentifierExpression("a").setDataType(IDataType.LONG)),
                new FunctionExpression(Functions.getInstance().getFunction("sum"), new IdentifierExpression("a").setDataType(IDataType.LONG))
            )
        );
        Assertions.assertEquals("IF( sum(a) <> 0, sum(a) / sum(a), 0)", expr.serializeToText(IdentifierQuotaStrategy.NONE));
    }
}
