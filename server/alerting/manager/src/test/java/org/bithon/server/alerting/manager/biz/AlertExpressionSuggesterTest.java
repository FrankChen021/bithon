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

package org.bithon.server.alerting.manager.biz;

import org.bithon.server.commons.autocomplete.Suggestion;
import org.bithon.server.datasource.DefaultSchema;
import org.bithon.server.datasource.column.LongColumn;
import org.bithon.server.datasource.column.StringColumn;
import org.bithon.server.web.service.datasource.api.DisplayableText;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/4/14 20:55
 */
public class AlertExpressionSuggesterTest {

    private final DefaultSchema eventSchema = new DefaultSchema("event",
                                                                "event",
                                                                null,
                                                                Arrays.asList(new StringColumn("appName", "appName"), new StringColumn("instanceName", "instanceName")),
                                                                Collections.singletonList(new LongColumn("eventCount", "eventCount")));

    private Collection<String> suggest(AlertExpressionSuggester suggester, String input) {
        return suggester.suggest(input)
                        .stream()
                        .map(Suggestion::getText)
                        .sorted()
                        .collect(Collectors.toList());
    }

    @Test
    public void testSuggestEmptyInput() {
        AlertExpressionSuggester suggester = new AlertExpressionSuggester(null);

        Assertions.assertEquals(Stream.of("(", // Start of expression
                                      // Aggregators
                                      "sum", "avg", "count", "max", "min")
                                  .sorted()
                                  .collect(Collectors.toList()),
                            suggest(suggester, ""));
    }

    @Test
    public void testSuggestAfterAggregator() {
        AlertExpressionSuggester suggester = new AlertExpressionSuggester(null);

        Assertions.assertEquals(Stream.of("(")
                                      .collect(Collectors.toList()),
                                suggest(suggester, "sum"));
    }

    @Test
    public void testSuggestAfterAggregatorExpression() {
        AlertExpressionSuggester suggester = new AlertExpressionSuggester(null);

        // TODO: BUGGY, should not suggest )
        Assertions.assertEquals(Stream.of("!=", ")", "<", "<=", "<>", "=", ">", ">=", "and", "by", "is", "or")
                                  .sorted()
                                  .collect(Collectors.toList()),
                            suggest(suggester, "sum (event.count) "));
    }

    @Test
    public void testSuggestAfterBY() {
        AlertExpressionSuggester suggester = new AlertExpressionSuggester(null);

        Assertions.assertEquals(Stream.of("(")
                                  .collect(Collectors.toList()),
                            suggest(suggester, "sum (event.count) BY"));
    }

    @Test
    public void testSuggestAfterBYExpression() {
        IDataSourceApi dataSourceApi = Mockito.mock(IDataSourceApi.class);
        Mockito.when(dataSourceApi.getSchemaByName("event"))
               .thenReturn(eventSchema);

        AlertExpressionSuggester suggester = new AlertExpressionSuggester(dataSourceApi);

        Assertions.assertEquals(Stream.of("appName", "instanceName")
                                  .sorted()
                                  .collect(Collectors.toList()),
                            suggest(suggester, "sum (event.count) BY ("));

        Assertions.assertEquals(Stream.of("appName", "instanceName")
                                  .sorted()
                                  .collect(Collectors.toList()),
                            suggest(suggester, "sum (event.count) BY (appName,"));

        Assertions.assertEquals(Stream.of("appName", "instanceName")
                                  .sorted()
                                  .collect(Collectors.toList()),
                            suggest(suggester, "sum (event.count) BY (appName, appName,"));

        Assertions.assertEquals(Stream.of("appName", "instanceName")
                                  .sorted()
                                  .collect(Collectors.toList()),
                            suggest(suggester, "sum (event.count) BY (appName, appName, appName, "));

        Assertions.assertEquals(Stream.of(",", ")")
                                  .sorted()
                                  .collect(Collectors.toList()),
                            suggest(suggester, "sum (event.count) BY (appName, appName, appName"));
    }

    @Test
    public void testSuggestDataSource() {
        IDataSourceApi dataSourceApi = Mockito.mock(IDataSourceApi.class);
        Mockito.when(dataSourceApi.getSchemaNames())
               .thenReturn(Stream.of("d2", "d1")
                                 .map((s) -> new DisplayableText(s, s))
                                 .collect(Collectors.toList()));

        AlertExpressionSuggester suggester = new AlertExpressionSuggester(dataSourceApi);

        Assertions.assertEquals(Arrays.asList("d1", "d2"),
                            suggest(suggester, "sum (\t"));
    }

    @Test
    public void testSuggestMetricNames() {
        IDataSourceApi dataSourceApi = Mockito.mock(IDataSourceApi.class);
        Mockito.when(dataSourceApi.getSchemaByName("event"))
               .thenReturn(eventSchema);

        AlertExpressionSuggester suggester = new AlertExpressionSuggester(dataSourceApi);

        Assertions.assertEquals(Collections.singletonList("eventCount"),
                            suggest(suggester, "sum (event. "));
    }

    @Test
    public void testSuggestAfterStartOfFilter() {
        IDataSourceApi dataSourceApi = Mockito.mock(IDataSourceApi.class);
        Mockito.when(dataSourceApi.getSchemaByName("event"))
               .thenReturn(eventSchema);

        AlertExpressionSuggester suggester = new AlertExpressionSuggester(dataSourceApi);
        Collection<String> suggestions = suggest(suggester, "sum(event.count{");

        // dimensions and end-of-filter are suggested
        Assertions.assertEquals(Arrays.asList("appName", "instanceName", "}"), suggestions);
    }

    @Test
    public void testSuggestFilterOperator() {
        IDataSourceApi dataSourceApi = Mockito.mock(IDataSourceApi.class);
        Mockito.when(dataSourceApi.getSchemaByName("event"))
               .thenReturn(eventSchema);

        AlertExpressionSuggester suggester = new AlertExpressionSuggester(dataSourceApi);
        Collection<String> suggestions = suggest(suggester, "sum(event.count{app");

        // TODO: BUG, should suggest startsWith, contains
        Assertions.assertEquals(Arrays.asList("!=", "!~", "<", "<=", "<>", "=", "=~", ">", ">=", "endswith", "hastoken", "in", "not"), suggestions);
    }

    @Test
    public void testSuggestFilterNOTOperator() {
        IDataSourceApi dataSourceApi = Mockito.mock(IDataSourceApi.class);
        Mockito.when(dataSourceApi.getSchemaByName("event"))
               .thenReturn(eventSchema);

        AlertExpressionSuggester suggester = new AlertExpressionSuggester(dataSourceApi);
        Collection<String> suggestions = suggest(suggester, "sum(event.count{app not");

        // BUG: should also suggest startsWith, contains
        Assertions.assertEquals(Arrays.asList("endswith", "hastoken", "in"), suggestions);
    }

    @Test
    public void testSuggestAfterOneFilter() {
        IDataSourceApi dataSourceApi = Mockito.mock(IDataSourceApi.class);
        Mockito.when(dataSourceApi.getSchemaByName("event"))
               .thenReturn(eventSchema);

        AlertExpressionSuggester suggester = new AlertExpressionSuggester(dataSourceApi);
        Collection<String> suggestions = suggest(suggester, "sum(event.count{appName='a'");
        Assertions.assertEquals(Arrays.asList(",", "}"), suggestions);
    }

    @Test
    public void testSuggestAfterMoreFilter() {
        IDataSourceApi dataSourceApi = Mockito.mock(IDataSourceApi.class);
        Mockito.when(dataSourceApi.getSchemaByName("event"))
               .thenReturn(eventSchema);

        AlertExpressionSuggester suggester = new AlertExpressionSuggester(dataSourceApi);
        Collection<String> suggestions = suggest(suggester, "sum(event.count{appName='a',");
        Assertions.assertEquals(Arrays.asList("appName", "instanceName"), suggestions);
    }

    @Test
    public void testSuggestAfterFilterCompletion() {
        AlertExpressionSuggester suggester = new AlertExpressionSuggester(null);
        Collection<String> suggestions = suggest(suggester, "sum(event.count{appName='a'}");
        Assertions.assertEquals(Collections.singletonList(")"), suggestions);
    }

    @Test
    public void testSuggestPredicate() {
        AlertExpressionSuggester suggester = new AlertExpressionSuggester(null);
        Collection<String> suggestions = suggest(suggester, "sum(event.count{appName='a'})");
        // TODO: buggy, SHOULD not suggest )
        Assertions.assertEquals(Stream.of("!=", ")", "<>", "=", ">", "<", ">=", "<=", "and", "by", "is", "or")
                                  .sorted()
                                  .collect(Collectors.toList()),
                            suggestions);
    }

    @Test
    public void testSuggestionNonAfterPredicate() {
        AlertExpressionSuggester suggester = new AlertExpressionSuggester(null);
        Collection<String> suggestions = suggest(suggester, "sum(event.count{appName='a'}) > ");
        Assertions.assertEquals(Collections.emptyList(),
                            suggestions);
    }

    @Test
    public void testSuggestionISPredicate() {
        /*
        AlertExpressionSuggester suggester = new AlertExpressionSuggester(null);
        Collection<String> suggestions = suggest(suggester, "sum(event.count{appName='a'}) IS ");
        Assertions.assertEquals(Arrays.asList("NULL"),
                            suggestions);
         */
    }

    @Test
    public void testSuggestionAfterCompleteExpression() {
        AlertExpressionSuggester suggester = new AlertExpressionSuggester(null);
        Collection<String> suggestions = suggest(suggester, "sum(event.count{appName='a'}) > 5 ");
        // TODO: buggy, should suggest 'and', 'or' only
        Assertions.assertEquals(Arrays.asList(")", "and", "or"),
                            suggestions);
    }

    @Test
    public void test_SuggestionAllColumnForCountAggregator() {
        IDataSourceApi dataSourceApi = Mockito.mock(IDataSourceApi.class);
        Mockito.when(dataSourceApi.getSchemaByName("event"))
               .thenReturn(eventSchema);

        AlertExpressionSuggester suggester = new AlertExpressionSuggester(dataSourceApi);
        List<Suggestion> suggestionList = suggester.suggest("count(event.")
                                                   .stream()
                                                   .toList();
        // all 3 columns are suggested
        Assertions.assertEquals(3, suggestionList.size());
        Assertions.assertEquals("appName", suggestionList.get(0).getText());
        Assertions.assertEquals("Dimension", ((AlertExpressionSuggester.SuggestionTag) suggestionList.get(0).getTag()).getTagText());

        Assertions.assertEquals("eventCount", suggestionList.get(1).getText());
        Assertions.assertEquals("Metric", ((AlertExpressionSuggester.SuggestionTag) suggestionList.get(1).getTag()).getTagText());

        Assertions.assertEquals("instanceName", suggestionList.get(2).getText());
        Assertions.assertEquals("Dimension", ((AlertExpressionSuggester.SuggestionTag) suggestionList.get(2).getTag()).getTagText());
    }
}
