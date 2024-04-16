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
import org.bithon.server.storage.datasource.DefaultSchema;
import org.bithon.server.storage.datasource.column.LongColumn;
import org.bithon.server.storage.datasource.column.StringColumn;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/4/14 20:55
 */
public class AlertExpressionSuggesterTest {

    @Test
    public void testSuggestEmptyInput() {
        AlertExpressionSuggester suggester = new AlertExpressionSuggester(null);

        Assert.assertEquals(Arrays.asList("(", // Start of expression
                                          // Aggregators
                                          "sum", "avg", "count", "max", "min")
                                  .stream()
                                  .sorted()
                                  .collect(Collectors.toList()),
                            suggest(suggester, ""));
    }

    @Test
    public void testSuggestAfterAggregator() {
        AlertExpressionSuggester suggester = new AlertExpressionSuggester(null);

        Assert.assertEquals(Arrays.asList("(", // Start of metric
                                          // GROUP BY
                                          "BY")
                                  .stream()
                                  .sorted()
                                  .collect(Collectors.toList()),
                            suggest(suggester, "sum"));
    }

    @Test
    public void testSuggestAfterBY() {
        AlertExpressionSuggester suggester = new AlertExpressionSuggester(null);

        // No suggestion because the datasource is not known at this stage
        Assert.assertEquals(Collections.emptyList(),
                            suggest(suggester, "sum BY ("));
    }

    @Test
    public void testSuggestFilter() {
        IDataSourceApi dataSourceApi = Mockito.mock(IDataSourceApi.class);
        Mockito.when(dataSourceApi.getSchemaByName("event"))
               .thenReturn(new DefaultSchema("event",
                                             "event",
                                             null,
                                             Arrays.asList(new StringColumn("appName", "appName"), new StringColumn("instanceName", "instanceName")),
                                             Collections.singletonList(new LongColumn("eventCount", "eventCount"))));

        AlertExpressionSuggester suggester = new AlertExpressionSuggester(dataSourceApi);
        {
            Collection<String> suggestions = suggest(suggester, "sum(event.count{");
            Assert.assertEquals(Arrays.asList("appName", "instanceName", "}"), suggestions);
        }

        {
            Collection<String> suggestions = suggest(suggester, "sum(event.count{appName='a'");
            Assert.assertEquals(Arrays.asList(",", "}"), suggestions);
        }

        {
            Collection<String> suggestions = suggest(suggester, "sum(event.count{appName='a',");
            Assert.assertEquals(Arrays.asList("appName", "instanceName"), suggestions);
        }
    }

    private Collection<String> suggest(AlertExpressionSuggester suggester, String input) {
        return suggester.suggest(input)
                        .stream()
                        .map(Suggestion::getText)
                        .sorted()
                        .collect(Collectors.toList());
    }
}
