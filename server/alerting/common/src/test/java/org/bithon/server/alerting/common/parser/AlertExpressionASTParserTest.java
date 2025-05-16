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

package org.bithon.server.alerting.common.parser;

import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Verify alert expression syntax
 *
 * @author frank.chen021@outlook.com
 * @date 2024/1/7 22:34
 */
public class AlertExpressionASTParserTest {

    @Test
    public void test_SimpleExpression() {
        IExpression expression = AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] <> 1 ");
        Assertions.assertInstanceOf(AlertExpression.class, expression);
        Assertions.assertEquals("avg(jvm-metrics.cpu{appName <= \"a\"})[5m] <> 1", expression.serializeToText());

    }

    @Test
    public void test_CompoundExpression() {
        IExpression expression = AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] <> 1 " +
                                                                "AND " +
                                                                "avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 2 ");
        Assertions.assertInstanceOf(LogicalExpression.AND.class, expression);
        Assertions.assertEquals("1", ((AlertExpression) ((LogicalExpression.AND) expression).getOperands().get(0)).getId());
        Assertions.assertEquals("2", ((AlertExpression) ((LogicalExpression.AND) expression).getOperands().get(1)).getId());

        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 1 " +
                                       "AND avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 2 " +
                                       "OR avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 3");

        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 1 " +
                                       "AND (avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 2)" +
                                       "OR (avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 3)");

        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 3 " +
                                       "AND (" +
                                       "(avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 1) OR (avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 2) " +
                                       ")");

    }

    @Test
    public void test_ByExpression_OneHasBY() {
        IExpression expression = AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] BY (instanceName) <> 1 " +
                                                                "AND " +
                                                                "avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 2 ");
        Assertions.assertInstanceOf(LogicalExpression.AND.class, expression);
        Assertions.assertEquals("1", ((AlertExpression) ((LogicalExpression.AND) expression).getOperands().get(0)).getId());
        Assertions.assertEquals("2", ((AlertExpression) ((LogicalExpression.AND) expression).getOperands().get(1)).getId());
    }

    @Test
    public void test_ByExpression_ByExpressionAreTheSame() {
        IExpression expression = AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] BY (appName, instanceName) <> 1 " +
                                                                "AND " +
                                                                "avg(jvm-metrics.cpu{appName <= 'a'})[5m] BY (instanceName, appName) > 2 ");
        Assertions.assertInstanceOf(LogicalExpression.AND.class, expression);
        Assertions.assertEquals("1", ((AlertExpression) ((LogicalExpression.AND) expression).getOperands().get(0)).getId());
        Assertions.assertEquals("2", ((AlertExpression) ((LogicalExpression.AND) expression).getOperands().get(1)).getId());
    }

    @Test
    public void test_ByExpression_ByExpressionAreNOTSame() {
        Assertions.assertThrows(InvalidExpressionException.class, () -> AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] BY (appName) <> 1 " +
                                                                                                   "AND " +
                                                                                                   "avg(jvm-metrics.cpu{appName <= 'a'})[5m] BY (instanceName) > 2 "));
    }
}
