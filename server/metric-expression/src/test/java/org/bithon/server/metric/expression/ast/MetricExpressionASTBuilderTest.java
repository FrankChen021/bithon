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

package org.bithon.server.metric.expression.ast;

import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.component.commons.expression.serialization.IdentifierQuotaStrategy;
import org.bithon.component.commons.utils.HumanReadableNumber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

/**
 * Verify alert expression syntax
 *
 * @author frank.chen021@outlook.com
 * @date 2024/1/7 22:34
 */
public class MetricExpressionASTBuilderTest {

    @Test
    public void test_ColonCharacter() {
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu:usage{appName = 'a'}) > 1");
        Assertions.assertNotNull(expression);
        Assertions.assertEquals("jvm-metrics", expression.getFrom());
        Assertions.assertEquals("avg", expression.getMetric().getAggregator());
        Assertions.assertEquals("cpu:usage", expression.getMetric().getField());
        Assertions.assertEquals("avg(jvm-metrics.cpu:usage{appName = \"a\"}) > 1", expression.serializeToText());

        // colon is not allowed in label
        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{app:name = 'a'})[5m] > 1"));
    }

    @Test
    public void test_Expression() {
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName = 'a'}) > 1");
        Assertions.assertNotNull(expression);
        Assertions.assertEquals("jvm-metrics", expression.getFrom());
        Assertions.assertEquals("avg", expression.getMetric().getAggregator());
        Assertions.assertEquals("cpu", expression.getMetric().getField());

        Assertions.assertEquals("avg(jvm-metrics.cpu{appName = \"a\"}) > 1", expression.serializeToText());
    }

    @Test
    public void test_NoPredicateExpression() {
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName = 'a'})");
        Assertions.assertNotNull(expression);
        Assertions.assertEquals("jvm-metrics", expression.getFrom());
        Assertions.assertEquals("avg", expression.getMetric().getAggregator());
        Assertions.assertEquals("cpu", expression.getMetric().getField());
    }

    @Test
    public void test_EmptyWhereExpression() {
        IExpression expression = MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu) > 1%");
        Assertions.assertNotNull(expression);

        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{}) > 1%");
    }

    @Test
    public void test_Percentage() {
        IExpression expression = MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu) > 1%[-1m]");
        Assertions.assertNotNull(expression);

        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu) > 101%[-1m]");
    }

    @Test
    public void test_WithLabelSelectorExpression() {
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <> 'a'}) > 1");
        Assertions.assertNotNull(expression);
        Assertions.assertEquals("jvm-metrics", expression.getFrom());
        Assertions.assertEquals("avg", expression.getMetric().getAggregator());
        Assertions.assertEquals("cpu", expression.getMetric().getField());

        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName > 'a'}) > 1");
        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName >= 'a'}) > 1");
        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <> 'a'}) > 1");
        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName < 'a'}) > 1");
        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'}) > 1");

        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a', instanceName='127.0.0.1'}) > 1");
    }

    @Test
    public void test_DurationExpression() {
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 1");
        Assertions.assertNotNull(expression);
        Assertions.assertEquals(5, expression.getWindow().getDuration().toMinutes());
        Assertions.assertEquals(TimeUnit.MINUTES, expression.getWindow().getUnit());

        expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5h] > 1");
        Assertions.assertNotNull(expression);
        Assertions.assertEquals(5, expression.getWindow().getDuration().toHours());
        Assertions.assertEquals(TimeUnit.HOURS, expression.getWindow().getUnit());

        // the duration must be a positive value
        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName in ('a', 1)})[0m] > 1"));
        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName in ('a', 1)})[-5m] > 1"));
    }

    @Test
    public void test_HumanReadableSizeExpression() {
        // binary format
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] > 1MiB");
        Assertions.assertNotNull(expression);
        Assertions.assertEquals(5, expression.getWindow().getDuration().toMinutes());
        Assertions.assertEquals(TimeUnit.MINUTES, expression.getWindow().getUnit());
        Assertions.assertEquals(HumanReadableNumber.of("1MiB"), expression.getExpected().getValue());
        Assertions.assertEquals("avg(jvm-metrics.cpu{appName <= \"a\"})[5m] > 1MiB", expression.serializeToText());

        // decimal format
        expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5h] > 7K");
        Assertions.assertNotNull(expression);
        Assertions.assertEquals(5, expression.getWindow().getDuration().toHours());
        Assertions.assertEquals(HumanReadableNumber.of("7K"), expression.getExpected().getValue());
        Assertions.assertEquals("avg(jvm-metrics.cpu{appName <= \"a\"})[5h] > 7K", expression.serializeToText());

        // simplified binary format
        expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5h] > 100Gi");
        Assertions.assertNotNull(expression);
        Assertions.assertEquals(5, expression.getWindow().getDuration().toHours());
        Assertions.assertEquals(HumanReadableNumber.of("100Gi"), expression.getExpected().getValue());
        Assertions.assertEquals("avg(jvm-metrics.cpu{appName <= \"a\"})[5h] > 100Gi", expression.serializeToText());

        // Invalid human-readable size
        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName in ('a', 1)})[0m] > 1MB"));
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
        Assertions.assertEquals("appName contains 'a'", expression.getLabelSelectorExpression().serializeToText(IdentifierQuotaStrategy.NONE));

        // contains require string literal
        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName contains 5})[5m]"));
    }

    @Test
    public void test_StartsWithPredicateExpression() {
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName startsWith 'a'})[5m] is null");
        Assertions.assertEquals("appName startsWith 'a'", expression.getLabelSelectorExpression().serializeToText(IdentifierQuotaStrategy.NONE));

        // startsWith require string literal
        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName startsWith 5})[5m]"));
    }

    @Test
    public void test_EnsWithPredicateExpression() {
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName endsWith 'a'})[5m] is null");
        Assertions.assertEquals("appName endsWith 'a'", expression.getLabelSelectorExpression().serializeToText(IdentifierQuotaStrategy.NONE));

        // startsWith require string literal
        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName endsWith 5})[5m]"));
    }

    @Test
    public void test_hasTokenPredicateExpression() {
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName hasToken 'a', instanceName hasToken '192.'})[5m] > 1");
        Assertions.assertEquals("(appName hasToken 'a') AND (instanceName hasToken '192.')", expression.getLabelSelectorExpression().serializeToText(IdentifierQuotaStrategy.NONE));

        // hasToken require string literal
        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName hasToken 5})[5m] > 1"));
    }

    @Test
    public void test_RegexMatchPredicateExpression() {
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{instanceName =~ '192.'})[5m] > 1");
        Assertions.assertEquals("instanceName =~ '192.'", expression.getLabelSelectorExpression().serializeToText(IdentifierQuotaStrategy.NONE));

        // hasToken require string literal
        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{instanceName =~ ab})[5m] > 1"));
    }

    @Test
    public void test_RegexNotMatchPredicateExpression() {
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{instanceName !~ '192.'})[5m] > 1");
        Assertions.assertEquals("instanceName !~ '192.'", expression.getLabelSelectorExpression().serializeToText(IdentifierQuotaStrategy.NONE));

        // hasToken require string literal
        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{instanceName !~ ab})[5m] > 1"));
    }

    @Test
    public void test_NotPredicateExpression() {
        // not contains
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName not contains 'a'})[5m] is null");
        Assertions.assertEquals("NOT (appName contains 'a')", expression.getLabelSelectorExpression().serializeToText(IdentifierQuotaStrategy.NONE));

        // not startsWith
        expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName not startsWith 'a'})[5m] is null");
        Assertions.assertEquals("NOT (appName startsWith 'a')", expression.getLabelSelectorExpression().serializeToText(IdentifierQuotaStrategy.NONE));

        // not endsWith
        expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName not endsWith 'a'})[5m] is null");
        Assertions.assertEquals("NOT (appName endsWith 'a')", expression.getLabelSelectorExpression().serializeToText(IdentifierQuotaStrategy.NONE));

        // not hasToken
        expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName not hasToken 'a'})[5m] is null");
        Assertions.assertEquals("NOT (appName hasToken 'a')", expression.getLabelSelectorExpression().serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_IsNullExpression() {
        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] is null");

        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] is 0"));
        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] > null"));
        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] >= null"));

        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] < null"));
        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] <= null"));

        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] <> null"));
        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName <= 'a'})[5m] != null"));
    }

    @Test
    public void test_InExpression() {
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName in ('a')})[5m] is null");
        Assertions.assertEquals("appName in ('a')", expression.getLabelSelectorExpression().serializeToText(IdentifierQuotaStrategy.NONE));

        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName in ('a', 'b')})[5m] is null");
        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName in (1)})[5m] is null");
        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName in (1,2)})[5m] is null");

        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName in ('a', 1)})[5m] is null"));
    }

    @Test
    public void test_NotInExpression() {
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName not in ('a')})[5m] is null");
        Assertions.assertEquals("appName not in ('a')", expression.getLabelSelectorExpression().serializeToText(IdentifierQuotaStrategy.NONE));

        MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName not in ('a', 'b')})[5m] is null");

        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName not in ('a', 1)})[5m] is null"));
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

        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("hhh(jvm-metrics.cpu{appName in ('a', 1)})[-5m] > 1"));
    }

    @Test
    public void test_OffsetExpression() {
        MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName contains 'a', instanceName contains '192.'})[5m] > 1%[-7m]");
        Assertions.assertNotNull(expression.getOffset());
        Assertions.assertEquals(-7, expression.getOffset().getDuration().toMinutes());

        // Only percentage is supported now
        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName contains 'a', instanceName contains '192.'})[5m] > 1[0m]"));

        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName contains 'a', instanceName contains '192.'})[5m] > 1%[0m]"));
        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName contains 'a', instanceName contains '192.'})[5m] > 1%[1m]"));

        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName contains 'a', instanceName contains '192.'})[5m] is null[-5m]"));
    }

    @Test
    public void test_ExpressionSerialization() {
        // No filter
        {
            MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu)[5m] > 10%[-7m]");
            Assertions.assertEquals("avg(jvm-metrics.cpu)[5m] > 10%[-7m]",
                                    expression.serializeToText(IdentifierQuotaStrategy.NONE));
        }

        // One filter
        {
            MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName = 'a'})[5m] > 10%[-5m]");
            Assertions.assertEquals("avg(jvm-metrics.cpu{appName = \"a\"})[5m] > 10%[-5m]",
                                    expression.serializeToText(IdentifierQuotaStrategy.NONE));
        }

        // Two filters
        {
            MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(jvm-metrics.cpu{appName contains 'a', instanceName contains '192.'})[5m] > 10%[-5m]");
            Assertions.assertEquals("avg(jvm-metrics.cpu{appName contains \"a\", instanceName contains \"192.\"})[5m] > 10%[-5m]",
                                    expression.serializeToText(IdentifierQuotaStrategy.NONE));
        }


        // count aggregator
        {
            MetricExpression expression = (MetricExpression) MetricExpressionASTBuilder.parse("count(   jvm-metrics.cpu{appName contains 'a', instanceName contains '192.'})[5m]  >  1");
            Assertions.assertEquals("count(jvm-metrics.cpu{appName contains \"a\", instanceName contains \"192.\"})[5m] > 1",
                                    expression.serializeToText());
        }
    }

    @Test
    public void test_InvalidMetricName() {
        // Metric name should be xxx.xxx
        Assertions.assertThrows(InvalidExpressionException.class, () -> MetricExpressionASTBuilder.parse("avg(jvm)[5m] > 1[-7m]"));
    }

    @Test
    public void test_MultipleSelector() {
        MetricExpression alertExpression = (MetricExpression) MetricExpressionASTBuilder.parse("avg(http-metrics.responseTime{appName='a', instance='localhost', url='http://localhost/test'})[5m] > 1%[-7m]");
        IExpression whereExpression = alertExpression.getLabelSelectorExpression();
        Assertions.assertEquals("(appName = 'a') AND (instance = 'localhost') AND (url = 'http://localhost/test')", whereExpression.serializeToText(IdentifierQuotaStrategy.NONE));
    }

    @Test
    public void test_ByExpression() {
        MetricExpression expr = (MetricExpression) MetricExpressionASTBuilder.parse("avg (http-metrics.responseTime{appName='a'})[5m] by (instance) > 1");
        Assertions.assertEquals(Collections.singleton("instance"), expr.getGroupBy());

        expr = (MetricExpression) MetricExpressionASTBuilder.parse("avg (http-metrics.responseTime{appName='a'})[5m] by (instance, url) > 1");
        Assertions.assertEquals(new HashSet<>(Arrays.asList("instance", "url")), expr.getGroupBy());

        expr = (MetricExpression) MetricExpressionASTBuilder.parse("avg (http-metrics.responseTime{appName='a'})[5m] by (instance, url, method) > 1");
        Assertions.assertEquals(new HashSet<>(Arrays.asList("instance", "url", "method")), expr.getGroupBy());
    }

    @Test
    public void test_SingleQuoteInLabel() {
        Assertions.assertEquals("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] > 0",
                                MetricExpressionASTBuilder.parse("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] > 0")
                                                          .serializeToText());
        Assertions.assertEquals("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] > 0",
                                MetricExpressionASTBuilder.parse("avg(jvm-metrics.activeThreads{appName = 'bithon-web-\\'local'})[1m] > 0").serializeToText());
    }

    @Test
    public void test_DoubleQuoteInLabel() {
        Assertions.assertEquals("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] > 0",
                                MetricExpressionASTBuilder.parse("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] > 0")
                                                          .serializeToText());

        Assertions.assertEquals("avg(jvm-metrics.activeThreads{appName = \"bithon-web-\\\"local\"})[1m] > 0",
                                MetricExpressionASTBuilder.parse("avg(jvm-metrics.activeThreads{appName = \"bithon-web-\\\"local\"})[1m] > 0")
                                                          .serializeToText());

    }

    @Test
    public void test_NegativePercentage() {
        Assertions.assertEquals("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] > -5%[-1d]",
                                MetricExpressionASTBuilder.parse("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] > -5%[-1d]")
                                                          .serializeToText());
    }

    @Test
    public void test_ADD_Literal() {
        {
            String expr = "avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 5";
            IExpression ast = MetricExpressionASTBuilder.parse(expr);
            Assertions.assertInstanceOf(ArithmeticExpression.class, ast);
            Assertions.assertEquals(expr, ast.serializeToText());
        }

        // Human readable literal
        {
            String expr = "avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 5MiB";
            IExpression ast = MetricExpressionASTBuilder.parse(expr);
            Assertions.assertInstanceOf(ArithmeticExpression.class, ast);
            Assertions.assertEquals(expr, ast.serializeToText());
        }

        // Human readable literal
        {
            String expr = "avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 5Mi";
            IExpression ast = MetricExpressionASTBuilder.parse(expr);
            Assertions.assertInstanceOf(ArithmeticExpression.class, ast);
            Assertions.assertEquals(expr, ast.serializeToText());
        }

        // Human readable literal
        {
            String expr = "avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 5G";
            IExpression ast = MetricExpressionASTBuilder.parse(expr);
            Assertions.assertInstanceOf(ArithmeticExpression.class, ast);
            Assertions.assertEquals(expr, ast.serializeToText());
        }

        // Percentage literal
        {
            String expr = "avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 5%";
            IExpression ast = MetricExpressionASTBuilder.parse(expr);
            Assertions.assertInstanceOf(ArithmeticExpression.class, ast);
            Assertions.assertEquals(expr, ast.serializeToText());
        }

        // duration literal
        {
            String expr = "avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] + 5h";
            IExpression ast = MetricExpressionASTBuilder.parse(expr);
            Assertions.assertInstanceOf(ArithmeticExpression.class, ast);
            Assertions.assertEquals(expr, ast.serializeToText());
        }
    }

    /**
     * test -5 in the expression below is properly parsed either as subtraction or as negative number
     */
    @Test
    public void test_Negative_and_Sub() {
        {
            String expr = "avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] -5";
            IExpression ast = MetricExpressionASTBuilder.parse(expr);
            Assertions.assertInstanceOf(ArithmeticExpression.class, ast);
            Assertions.assertEquals("avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] - 5", ast.serializeToText());
        }
        {
            String expr = "- 5";
            IExpression ast = MetricExpressionASTBuilder.parse(expr);
            Assertions.assertInstanceOf(LiteralExpression.class, ast);
            Assertions.assertEquals("-5", ast.serializeToText());
        }
        {
            String expr = "1 - 5";
            IExpression ast = MetricExpressionASTBuilder.parse(expr);
            Assertions.assertInstanceOf(ArithmeticExpression.class, ast);
            Assertions.assertEquals("1 - 5", ast.serializeToText());
        }
    }

    @Test
    public void test_SUB_Literal() {
        String expr = "avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] - 5";
        IExpression ast = MetricExpressionASTBuilder.parse(expr);
        Assertions.assertInstanceOf(ArithmeticExpression.class, ast);
        Assertions.assertEquals(expr, ast.serializeToText());
    }

    @Test
    public void test_SUB_BY_Literal() {
        String expr = "avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] BY (appName) - 5";
        IExpression ast = MetricExpressionASTBuilder.parse(expr);
        Assertions.assertInstanceOf(ArithmeticExpression.class, ast);
        Assertions.assertEquals(expr, ast.serializeToText());
    }

    @Test
    public void test_MUL_Literal() {
        String expr = "avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] / 5";
        IExpression ast = MetricExpressionASTBuilder.parse(expr);
        Assertions.assertInstanceOf(ArithmeticExpression.class, ast);
        Assertions.assertEquals(expr, ast.serializeToText());
    }

    @Test
    public void test_DIV_Literal() {
        String expr = "avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] / 5";
        IExpression ast = MetricExpressionASTBuilder.parse(expr);
        Assertions.assertInstanceOf(ArithmeticExpression.class, ast);
        Assertions.assertEquals(expr, ast.serializeToText());
    }

    @Test
    public void test_DIV_BY_Zero() {
        String expr = "avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] / 0";

        Assertions.assertThrows(ArithmeticException.class, () -> MetricExpressionASTBuilder.parse(expr));
    }

    @Test
    public void test_HybridExpression() {
        String expr = "avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] * "
                      + "(avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m] - avg(jvm-metrics.activeThreads{appName = \"bithon-web-'local\"})[1m])";
        IExpression ast = MetricExpressionASTBuilder.parse(expr);

        Assertions.assertInstanceOf(ArithmeticExpression.MUL.class, ast);
        ArithmeticExpression.MUL mul = (ArithmeticExpression.MUL) ast;
        Assertions.assertInstanceOf(MetricExpression.class, mul.getLhs());
        Assertions.assertInstanceOf(ArithmeticExpression.SUB.class, mul.getRhs());
        Assertions.assertEquals(expr, ast.serializeToText());
    }

    @Test
    public void test_ArithmeticPrecedence() {
        String expr = "5 + 3 * 4";
        IExpression ast = MetricExpressionASTBuilder.parse(expr);
        Assertions.assertEquals("5 + (3 * 4)", ast.serializeToText());
    }

    @Test
    public void test_ArithmeticPrecedence_2() {
        String expr = "4 * 5 + 6";
        IExpression ast = MetricExpressionASTBuilder.parse(expr);
        Assertions.assertEquals("(4 * 5) + 6", ast.serializeToText());
    }
}
