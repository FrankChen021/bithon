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

package org.bithon.server.metric.expression;

import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.component.commons.utils.HumanReadableNumber;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Verify alert expression syntax
 *
 * @author frank.chen021@outlook.com
 * @date 2024/1/7 22:34
 */
public class MetricExpressionASTBuilderTest {

    @Test
    public void test_Expression() {
        IExpression expression = MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName = 'a'}) > 1");
        Assert.assertTrue(expression instanceof MetricExpression);
        Assert.assertEquals("jvm-metrics", ((MetricExpression) expression).getFrom());
        Assert.assertEquals("avg", ((MetricExpression) expression).getMetric().getAggregator());
        Assert.assertEquals("cpu", ((MetricExpression) expression).getMetric().getField());

        Assert.assertEquals("avg(jvm-metrics.cpu{appName = \"a\"}) > 1", expression.serializeToText());
    }

    @Test
    public void test_NoPredicateExpression() {
        IExpression expression = MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName = 'a'})");
        Assert.assertTrue(expression instanceof MetricExpression);
        Assert.assertEquals("jvm-metrics", ((MetricExpression) expression).getFrom());
        Assert.assertEquals("avg", ((MetricExpression) expression).getMetric().getAggregator());
        Assert.assertEquals("cpu", ((MetricExpression) expression).getMetric().getField());
    }

    @Test
    public void test_EmptyWhereExpression() {
        IExpression expression = MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu) > 1%");
        Assert.assertTrue(expression instanceof MetricExpression);

        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{}) > 1%");
    }

    @Test
    public void test_Percentage() {
        IExpression expression = MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu) > 1%[-1m]");
        Assert.assertTrue(expression instanceof MetricExpression);

        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu) > 101%[-1m]");
    }

    @Test
    public void test_WithLabelSelectorExpression() {
        IExpression expression = MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <> 'a'}) > 1");
        Assert.assertTrue(expression instanceof MetricExpression);
        Assert.assertEquals("jvm-metrics", ((MetricExpression) expression).getFrom());
        Assert.assertEquals("avg", ((MetricExpression) expression).getMetric().getAggregator());
        Assert.assertEquals("cpu", ((MetricExpression) expression).getMetric().getField());

        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName > 'a'}) > 1");
        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName >= 'a'}) > 1");
        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <> 'a'}) > 1");
        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName < 'a'}) > 1");
        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'}) > 1");

        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a', instanceName='127.0.0.1'}) > 1");
    }

    @Test
    public void test_DurationExpression() {
        IExpression expression = MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 1");
        Assert.assertTrue(expression instanceof MetricExpression);
        Assert.assertEquals(5, ((MetricExpression) expression).getWindow().getDuration().toMinutes());
        Assert.assertEquals(TimeUnit.MINUTES, ((MetricExpression) expression).getWindow().getUnit());

        expression = MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5h] > 1");
        Assert.assertTrue(expression instanceof MetricExpression);
        Assert.assertEquals(5, ((MetricExpression) expression).getWindow().getDuration().toHours());
        Assert.assertEquals(TimeUnit.HOURS, ((MetricExpression) expression).getWindow().getUnit());

        // the duration must be a positive value
        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName in ('a', 1)})[0m] > 1"));
        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName in ('a', 1)})[-5m] > 1"));
    }

    @Test
    public void test_HumanReadableSizeExpression() {
        // binary format
        IExpression expression = MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 1MiB");
        Assert.assertTrue(expression instanceof MetricExpression);
        Assert.assertEquals(5, ((MetricExpression) expression).getWindow().getDuration().toMinutes());
        Assert.assertEquals(TimeUnit.MINUTES, ((MetricExpression) expression).getWindow().getUnit());
        Assert.assertEquals(HumanReadableNumber.of("1MiB"), ((MetricExpression) expression).getExpected().getValue());
        Assert.assertEquals("avg(jvm-metrics.cpu{appName <= \"a\"})[5m] > 1MiB", expression.serializeToText());

        // decimal format
        expression = MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5h] > 7K");
        Assert.assertTrue(expression instanceof MetricExpression);
        Assert.assertEquals(5, ((MetricExpression) expression).getWindow().getDuration().toHours());
        Assert.assertEquals(HumanReadableNumber.of("7K"), ((MetricExpression) expression).getExpected().getValue());
        Assert.assertEquals("avg(jvm-metrics.cpu{appName <= \"a\"})[5h] > 7K", expression.serializeToText());

        // simplified binary format
        expression = MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5h] > 100Gi");
        Assert.assertTrue(expression instanceof MetricExpression);
        Assert.assertEquals(5, ((MetricExpression) expression).getWindow().getDuration().toHours());
        Assert.assertEquals(HumanReadableNumber.of("100Gi"), ((MetricExpression) expression).getExpected().getValue());
        Assert.assertEquals("avg(jvm-metrics.cpu{appName <= \"a\"})[5h] > 100Gi", expression.serializeToText());

        // Invalid human readable size
        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName in ('a', 1)})[0m] > 1MB"));
    }

    @Test
    public void test_SimplePredicateExpression() {
        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 1");
        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] >= 1");

        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] = 1");

        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] < 1");
        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] <= 1");

        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] <> 1");
        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] != 1");
    }

    @Test
    public void test_ContainsPredicateExpression() {
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName contains 'a'})[5m] is null");
        Assert.assertEquals("appName contains 'a'", expression.getLabelSelectorExpression().serializeToText(null));

        // contains require string literal
        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName contains 5})[5m]"));
    }

    @Test
    public void test_StartsWithPredicateExpression() {
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName startsWith 'a'})[5m] is null");
        Assert.assertEquals("appName startsWith 'a'", expression.getLabelSelectorExpression().serializeToText(null));

        // startsWith require string literal
        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName startsWith 5})[5m]"));
    }

    @Test
    public void test_EnsWithPredicateExpression() {
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName endsWith 'a'})[5m] is null");
        Assert.assertEquals("appName endsWith 'a'", expression.getLabelSelectorExpression().serializeToText(null));

        // startsWith require string literal
        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName endsWith 5})[5m]"));
    }

    @Test
    public void test_LikePredicateExpression() {
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName like 'a%', instanceName like '192.%'})[5m] > 1");
        Assert.assertEquals("(appName like 'a%' AND instanceName like '192.%')", expression.getLabelSelectorExpression().serializeToText(null));

        // like require string literal
        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName like 5})[5m] > 1"));
    }

    @Test
    public void test_NotPredicateExpression() {
        // not contains
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName not contains 'a'})[5m] is null");
        Assert.assertEquals("NOT (appName contains 'a')", expression.getLabelSelectorExpression().serializeToText(null));

        // not startsWith
        expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName not startsWith 'a'})[5m] is null");
        Assert.assertEquals("NOT (appName startsWith 'a')", expression.getLabelSelectorExpression().serializeToText(null));

        // not endsWith
        expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName not endsWith 'a'})[5m] is null");
        Assert.assertEquals("NOT (appName endsWith 'a')", expression.getLabelSelectorExpression().serializeToText(null));

        // not like
        expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName not like 'a'})[5m] is null");
        Assert.assertEquals("NOT (appName like 'a')", expression.getLabelSelectorExpression().serializeToText(null));
    }

    @Test
    public void test_IsNullExpression() {
        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] is null");

        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] is 0"));
        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] > null"));
        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] >= null"));

        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] < null"));
        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] <= null"));

        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] <> null"));
        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] != null"));
    }

    @Test
    public void test_InExpression() {
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName in ('a')})[5m] is null");
        Assert.assertEquals("appName in ('a')", expression.getLabelSelectorExpression().serializeToText(null));

        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName in ('a', 'b')})[5m] is null");
        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName in (1)})[5m] is null");
        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName in (1,2)})[5m] is null");

        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName in ('a', 1)})[5m] is null"));
    }

    @Test
    public void test_NotInExpression() {
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName not in ('a')})[5m] is null");
        Assert.assertEquals("appName not in ('a')", expression.getLabelSelectorExpression().serializeToText(null));

        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName not in ('a', 'b')})[5m] is null");

        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName not in ('a', 1)})[5m] is null"));
    }

    @Test
    public void test_AggregatorExpression() {
        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 1");
        MetricExpressionASTBuilder.parse("count(jvm-metrics.cpu{appName <= 'a'})[5m] > 1");
        MetricExpressionASTBuilder.parse("sum(jvm-metrics.cpu{appName <= 'a'})[5m] > 1");
        MetricExpressionASTBuilder.parse("min(jvm-metrics.cpu{appName <= 'a'})[5m] > 1");
        MetricExpressionASTBuilder.parse("max(jvm-metrics.cpu{appName <= 'a'})[5m] > 1");
        MetricExpressionASTBuilder.parse("first(jvm-metrics.cpu{appName <= 'a'})[5m] > 1");
        MetricExpressionASTBuilder.parse("last(jvm-metrics.cpu{appName <= 'a'})[5m] > 1");

        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("hhh(jvm-metrics.cpu{appName in ('a', 1)})[-5m] > 1"));
    }

    @Test
    public void test_ExpectedWindow() {
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName like 'a%', instanceName like '192.%'})[5m] > 1[-7m]");
        Assert.assertNotNull(expression.getExpectedWindow());
        Assert.assertEquals(-7, expression.getExpectedWindow().getDuration().toMinutes());

        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName like 'a%', instanceName like '192.%'})[5m] > 1[0m]"));
        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName like 'a%', instanceName like '192.%'})[5m] > 1[1m]"));

        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName like 'a%', instanceName like '192.%'})[5m] is null[-5m]"));
    }

    @Test
    public void test_ExpressionSerialization() {
        // No filter
        {
            MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu)[5m] > 1[-7m]");
            Assert.assertEquals("avg(jvm-metrics.cpu)[5m] > 1[-7m]",
                                expression.serializeToText(null));
        }

        // One filter
        {
            MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName = 'a'})[5m] > 1[-5m]");
            Assert.assertEquals("avg(jvm-metrics.cpu{appName = \"a\"})[5m] > 1[-5m]",
                                expression.serializeToText(null));
        }

        // Two filters
        {
            MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName like 'a%', instanceName like '192.%'})[5m] > 1[-5m]");
            Assert.assertEquals("avg(jvm-metrics.cpu{appName like \"a%\", instanceName like \"192.%\"})[5m] > 1[-5m]",
                                expression.serializeToText(null));
        }


        // count aggregator
        {
            MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("count(   jvm-metrics.cpu{appName like 'a%', instanceName like '192.%'})[5m]  >  1");
            Assert.assertEquals("count(jvm-metrics.cpu{appName like \"a%\", instanceName like \"192.%\"})[5m] > 1",
                                expression.serializeToText());
        }
    }

    @Test
    public void test_InvalidMetricName() {
        // Metric name should be xxx.xxx
        Assert.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm)[5m] > 1[-7m]"));
    }

    @Test
    public void test_MultipleSelector() {
        MetricExpression alertExpression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(http-metrics.responseTime{appName='a', instance='localhost', url='http://localhost/test'})[5m] > 1[-7m]");
        IExpression whereExpression = alertExpression.getLabelSelectorExpression();
        Assert.assertEquals("(appName = 'a' AND instance = 'localhost' AND url = 'http://localhost/test')", whereExpression.serializeToText(null));
    }

    @Test
    public void test_ByExpression() {
        MetricExpression expr = (MetricExpression) MetricExpressionASTBuilder.parse("avg (http-metrics.responseTime{appName='a'})[5m] by (instance) > 1");
        Assert.assertEquals(Collections.singletonList("instance"), expr.getGroupBy());

        expr = (MetricExpression) MetricExpressionASTBuilder.parse("avg (http-metrics.responseTime{appName='a'})[5m] by (instance, url) > 1");
        Assert.assertEquals(Arrays.asList("instance", "url"), expr.getGroupBy());

        expr = (MetricExpression) MetricExpressionASTBuilder.parse("avg (http-metrics.responseTime{appName='a'})[5m] by (instance, url, method) > 1");
        Assert.assertEquals(Arrays.asList("instance", "url", "method"), expr.getGroupBy());
    }
}
