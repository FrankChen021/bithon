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
import org.bithon.server.alerting.common.model.AlertExpression;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * Verify alert expression syntax
 *
 * @author frank.chen021@outlook.com
 * @date 2024/1/7 22:34
 */
public class AlertExpressionASTParserTest {

    @Test
    public void testExpression() {
        IExpression expression = AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName = 'a'}) > 1");
        Assert.assertTrue(expression instanceof AlertExpression);
        Assert.assertEquals("1", ((AlertExpression) expression).getId());
        Assert.assertEquals("jvm-metrics", ((AlertExpression) expression).getFrom());
        Assert.assertEquals("avg", ((AlertExpression) expression).getSelect().getAggregator());
        Assert.assertEquals("cpu", ((AlertExpression) expression).getSelect().getField());
        Assert.assertEquals(60, ((AlertExpression) expression).getWindow().getDuration().getSeconds());
    }

    @Test
    public void testEmptyWhereExpression() {
        IExpression expression = AlertExpressionASTParser.parse("avg(jvm-metrics.cpu) > 1%");
        Assert.assertTrue(expression instanceof AlertExpression);

        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{}) > 1%");
    }

    @Test
    public void testFilterExpression() {
        IExpression expression = AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <> 'a'}) > 1");
        Assert.assertTrue(expression instanceof AlertExpression);
        Assert.assertEquals("jvm-metrics", ((AlertExpression) expression).getFrom());
        Assert.assertEquals("avg", ((AlertExpression) expression).getSelect().getAggregator());
        Assert.assertEquals("cpu", ((AlertExpression) expression).getSelect().getField());
        Assert.assertEquals(60, ((AlertExpression) expression).getWindow().getDuration().getSeconds());

        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName > 'a'}) > 1");
        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName >= 'a'}) > 1");
        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <> 'a'}) > 1");
        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName < 'a'}) > 1");
        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'}) > 1");

        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a', instanceName='127.0.0.1'}) > 1");
    }

    @Test
    public void testDurationExpression() {
        IExpression expression = AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 1");
        Assert.assertTrue(expression instanceof AlertExpression);
        Assert.assertEquals(5, ((AlertExpression) expression).getWindow().getDuration().toMinutes());
        Assert.assertEquals(TimeUnit.MINUTES, ((AlertExpression) expression).getWindow().getUnit());

        expression = AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5h] > 1");
        Assert.assertTrue(expression instanceof AlertExpression);
        Assert.assertEquals(5, ((AlertExpression) expression).getWindow().getDuration().toHours());
        Assert.assertEquals(TimeUnit.HOURS, ((AlertExpression) expression).getWindow().getUnit());

        // the duration must be a positive value
        Assert.assertThrows(InvalidExpressionException.class, () -> AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName in ('a', 1)})[0m] > 1"));
        Assert.assertThrows(InvalidExpressionException.class, () -> AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName in ('a', 1)})[-5m] > 1"));
    }

    @Test
    public void testPredicateExpression() {
        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 1");
        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] >= 1");

        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] = 1");

        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] < 1");
        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] <= 1");

        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] <> 1");
        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] != 1");
    }

    @Test
    public void testCompoundExpression() {
        IExpression expression = AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] <> 1 " +
                                                                    "AND " +
                                                                    "avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 2 ");
        Assert.assertTrue(expression instanceof LogicalExpression.AND);
        Assert.assertEquals("1", ((AlertExpression) ((LogicalExpression.AND) expression).getOperands().get(0)).getId());
        Assert.assertEquals("2", ((AlertExpression) ((LogicalExpression.AND) expression).getOperands().get(1)).getId());

        expression = AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 1 " +
                                                        "AND avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 2 " +
                                                        "OR avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 3");

        expression = AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 1 " +
                                                        "AND (avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 2)" +
                                                        "OR (avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 3)");

        expression = AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 3 " +
                                                        "AND (" +
                                                        "(avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 1) OR (avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 2) " +
                                                        ")");

    }

    @Test
    public void testIsNullExpression() {
        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] is null");

        Assert.assertThrows(InvalidExpressionException.class, () -> AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] is 0"));
        Assert.assertThrows(InvalidExpressionException.class, () -> AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] > null"));
        Assert.assertThrows(InvalidExpressionException.class, () -> AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] >= null"));

        Assert.assertThrows(InvalidExpressionException.class, () -> AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] < null"));
        Assert.assertThrows(InvalidExpressionException.class, () -> AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] <= null"));

        Assert.assertThrows(InvalidExpressionException.class, () -> AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] <> null"));
        Assert.assertThrows(InvalidExpressionException.class, () -> AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] != null"));
    }

    @Test
    public void testInExpression() {
        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName in ('a')})[5m] is null");
        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName in ('a', 'b')})[5m] is null");
        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName in (1)})[5m] is null");
        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName in (1,2)})[5m] is null");

        Assert.assertThrows(InvalidExpressionException.class, () -> AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName in ('a', 1)})[5m] is null"));
    }

    @Test
    public void testNotInExpression() {
        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName not in ('a')})[5m] is null");
        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName not in ('a', 'b')})[5m] is null");

        Assert.assertThrows(InvalidExpressionException.class, () -> AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName not in ('a', 1)})[5m] is null"));
    }

    @Test
    public void testAggregatorExpression() {
        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 1");
        AlertExpressionASTParser.parse("count(jvm-metrics.cpu{appName <= 'a'})[5m] > 1");
        AlertExpressionASTParser.parse("sum(jvm-metrics.cpu{appName <= 'a'})[5m] > 1");
        AlertExpressionASTParser.parse("min(jvm-metrics.cpu{appName <= 'a'})[5m] > 1");
        AlertExpressionASTParser.parse("max(jvm-metrics.cpu{appName <= 'a'})[5m] > 1");
        AlertExpressionASTParser.parse("first(jvm-metrics.cpu{appName <= 'a'})[5m] > 1");
        AlertExpressionASTParser.parse("last(jvm-metrics.cpu{appName <= 'a'})[5m] > 1");

        Assert.assertThrows(InvalidExpressionException.class, () -> AlertExpressionASTParser.parse("hhh(jvm-metrics.cpu{appName in ('a', 1)})[-5m] > 1"));
    }

    @Test
    public void testLikeExpression() {
        AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName like 'a%', instanceName like '192.%'})[5m] > 1");

        Assert.assertThrows(InvalidExpressionException.class, () -> AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName like 5})[5m] > 1"));
    }

    @Test
    public void testExpectedWindow() {
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName like 'a%', instanceName like '192.%'})[5m] > 1[-7m]");
        Assert.assertNotNull(expression.getExpectedWindow());
        Assert.assertEquals(-7, expression.getExpectedWindow().getDuration().toMinutes());

        Assert.assertThrows(InvalidExpressionException.class, () -> AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName like 'a%', instanceName like '192.%'})[5m] > 1[0m]"));
        Assert.assertThrows(InvalidExpressionException.class, () -> AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName like 'a%', instanceName like '192.%'})[5m] > 1[1m]"));

        Assert.assertThrows(InvalidExpressionException.class, () -> AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName like 'a%', instanceName like '192.%'})[5m] is null[-5m]"));
    }

    @Test
    public void testExpressionSerialization() {
        // No filter
        {
            AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse("avg(jvm-metrics.cpu)[5m] > 1[-7m]");
            Assert.assertEquals("avg(jvm-metrics.cpu)[5m] > 1[-7m]",
                                expression.serializeToText(null));
        }

        // One filter
        {
            AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName = 'a'})[5m] > 1[-5m]");
            Assert.assertEquals("avg(jvm-metrics.cpu{appName = \"a\"})[5m] > 1[-5m]",
                                expression.serializeToText(null));
        }

        // Two filters
        {
            AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName like 'a%', instanceName like '192.%'})[5m] > 1[-5m]");
            Assert.assertEquals("avg(jvm-metrics.cpu{appName like \"a%\", instanceName like \"192.%\"})[5m] > 1[-5m]",
                                expression.serializeToText(null));
        }


        // count aggregator
        {
            AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse("count(   jvm-metrics.cpu{appName like 'a%', instanceName like '192.%'})[5m]  >  1");
            Assert.assertEquals("count(jvm-metrics.cpu{appName like \"a%\", instanceName like \"192.%\"})[5m] > 1",
                                expression.serializeToText(null));
        }
    }
}
