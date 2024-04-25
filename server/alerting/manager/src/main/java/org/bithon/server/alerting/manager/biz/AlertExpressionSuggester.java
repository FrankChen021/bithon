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

import com.google.common.collect.ImmutableSet;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.Token;
import org.bithon.server.alerting.common.parser.AlertExpressionLexer;
import org.bithon.server.alerting.common.parser.AlertExpressionParser;
import org.bithon.server.commons.autocomplete.AutoSuggesterBuilder;
import org.bithon.server.commons.autocomplete.CasePreference;
import org.bithon.server.commons.autocomplete.DefaultLexerAndParserFactory;
import org.bithon.server.commons.autocomplete.Suggestion;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.column.StringColumn;
import org.bithon.server.web.service.datasource.api.GetDimensionRequest;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/4/14 12:33
 */
@Slf4j
public class AlertExpressionSuggester {

    @Data
    @AllArgsConstructor
    public static class SuggestionTag {
        private String tagText;

        public static SuggestionTag of(String text) {
            return new SuggestionTag(text);
        }
    }

    private final IDataSourceApi dataSourceApi;
    private final AutoSuggesterBuilder suggesterBuilder;
    private final Set<Integer> predicateOperators = ImmutableSet.of(
        AlertExpressionParser.LT,
        AlertExpressionParser.LTE,
        AlertExpressionParser.GT,
        AlertExpressionParser.GTE,
        AlertExpressionParser.NE,
        AlertExpressionParser.EQ,
        AlertExpressionParser.IS,
        AlertExpressionParser.NOT,
        AlertExpressionParser.LIKE,
        AlertExpressionParser.HAS,
        AlertExpressionParser.CONTAINS,
        AlertExpressionParser.STARTWITH,
        AlertExpressionParser.ENDWITH);

    public AlertExpressionSuggester(IDataSourceApi dataSourceApi) {
        this.dataSourceApi = dataSourceApi;

        DefaultLexerAndParserFactory factory = new DefaultLexerAndParserFactory(
            AlertExpressionLexer.class,
            AlertExpressionParser.class
        );
        this.suggesterBuilder = AutoSuggesterBuilder.builder()
                                                    .factory(factory)
                                                    .casePreference(CasePreference.UPPER);

        this.suggesterBuilder.setSuggester(AlertExpressionParser.RULE_aggregatorExpression, (inputs, expectedToken, suggestions) -> {
            if (expectedToken.tokenType == AlertExpressionParser.IDENTIFIER) {
                suggestions.add(Suggestion.of(expectedToken.tokenType, "sum", SuggestionTag.of("Aggregator")));
                suggestions.add(Suggestion.of(expectedToken.tokenType, "count", SuggestionTag.of("Aggregator")));
                suggestions.add(Suggestion.of(expectedToken.tokenType, "avg", SuggestionTag.of("Aggregator")));
                suggestions.add(Suggestion.of(expectedToken.tokenType, "max", SuggestionTag.of("Aggregator")));
                suggestions.add(Suggestion.of(expectedToken.tokenType, "min", SuggestionTag.of("Aggregator")));
            }
            return false;
        });

        // Suggest column names for GROUP BY expression
        this.suggesterBuilder.setSuggester(AlertExpressionParser.RULE_groupByExpression, (inputs, expectedToken, suggestions) -> {
            if (expectedToken.tokenType != AlertExpressionParser.IDENTIFIER) {
                // Auto suggestion for keyword
                return true;
            }

            int index = inputs.size() - 1;

            // Find the DOT separator token which is after the data source name
            while (index > 0 && inputs.get(index).getType() != AlertExpressionParser.DOT) {
                --index;
            }

            // The data source is before the dot token
            index--;

            if (index > 0) {
                String dataSource = inputs.get(index).getText();
                ISchema schema = dataSourceApi.getSchemaByName(dataSource);
                if (schema != null) {
                    schema.getColumns()
                          .stream()
                          .filter((col) -> (col instanceof StringColumn))
                          .forEach(col -> suggestions.add(Suggestion.of(expectedToken.tokenType, col.getName(), SuggestionTag.of("Dimension"))));
                }
            }

            return false;
        });

        // Suggest data source names
        this.suggesterBuilder.setSuggester(AlertExpressionParser.RULE_dataSourceExpression, (inputs, expectedToken, suggestions) -> {
            dataSourceApi.getSchemaNames()
                         .forEach((value) -> suggestions.add(Suggestion.of(expectedToken.tokenType, value.getValue(), SuggestionTag.of("DataSource"))));
            return false;
        });

        // Suggest metric names for the metric name expression
        this.suggesterBuilder.setSuggester(AlertExpressionParser.RULE_metricNameExpression, (inputs, expectedToken, suggestions) -> {
            if (expectedToken.tokenType != AlertExpressionParser.IDENTIFIER) {
                return false;
            }

            // -1 is the DOT
            String dataSource = inputs.get(inputs.size() - 2).getText();
            ISchema schema = dataSourceApi.getSchemaByName(dataSource);
            if (schema != null) {
                schema.getColumns()
                      .stream()
                      .filter((col) -> !(col instanceof StringColumn)
                                       && !col.getName().equals(schema.getTimestampSpec().getColumnName()))
                      .forEach(col -> suggestions.add(Suggestion.of(expectedToken.tokenType, col.getName(), SuggestionTag.of("Metric"))));
            }

            return false;
        });

        this.suggesterBuilder.setSuggester(AlertExpressionParser.RULE_filterExpression, (inputs, expectedToken, suggestions) -> {
            if (expectedToken.tokenType != AlertExpressionParser.IDENTIFIER) {
                // So that operators can be suggested automatically
                return true;
            }

            // Find the filter starter
            int i = inputs.size() - 1;
            while (i > 0 && inputs.get(i).getType() != AlertExpressionParser.LEFT_CURLY_BRACE) {
                --i;
            }

            String dataSource = getDataSource(inputs, i);
            if (dataSource == null) {
                return false;
            }

            ISchema schema = dataSourceApi.getSchemaByName(dataSource);
            if (schema != null) {
                schema.getColumns()
                      .stream()
                      .filter((col) -> (col instanceof StringColumn))
                      .forEach(col -> suggestions.add(Suggestion.of(expectedToken.tokenType, col.getName(), SuggestionTag.of("Dimension"))));
            }

            return false;
        });

        this.suggesterBuilder.setSuggester(AlertExpressionParser.RULE_literalExpression, (inputs, expectedToken, suggestions) -> {
            // No suggestion for literal expressions except the NULL
            //if (expectedToken.expectedTokenType == AlertExpressionParser.NULL_LITERAL
            //    && !inputs.isEmpty()
            //    && inputs.get(inputs.size() - 1).getText().equalsIgnoreCase("IS")
            //) {
            //    suggestions.add(Suggestion.of(expectedToken.expectedTokenType, "NULL"));
            //}
            if (expectedToken.tokenType == AlertExpressionParser.STRING_LITERAL) {
                int index = inputs.size() - 1;
                while (index > 0 && this.predicateOperators.contains(inputs.get(index).getType())) {
                    index--;
                }

                if (index <= 0 || inputs.get(index).getType() != AlertExpressionParser.IDENTIFIER) {
                    return false;
                }
                String identifier = inputs.get(index).getText();

                // Make sure the literal is in the filter expression by finding the filter expression start
                while (index > 0 && inputs.get(index).getType() != AlertExpressionParser.LEFT_CURLY_BRACE) {
                    if (inputs.get(index).getType() == AlertExpressionParser.RIGHT_CURLY_BRACE) {
                        return false;
                    }

                    index--;
                }

                // Get the data source for further search
                String dataSource = getDataSource(inputs, index);
                if (dataSource == null) {
                    return false;
                }

                // Load suggestion for filter
                if ("appName".equals(identifier)) {
                    try {
                        // TODO: cache the search result
                        Collection<Map<String, String>> dims = dataSourceApi.getDimensions(GetDimensionRequest.builder()
                                                                                                              .dataSource(dataSource)
                                                                                                              .name(identifier)
                                                                                                              .startTimeISO8601(TimeSpan.now().floor(Duration.ofDays(1)).toISO8601())
                                                                                                              .endTimeISO8601(TimeSpan.now().ceil(Duration.ofHours(1)).toISO8601())
                                                                                                              .build());
                        for (Map<String, String> dim : dims) {
                            suggestions.add(Suggestion.of(expectedToken.tokenType, "'" + dim.get("value") + "'", SuggestionTag.of("Value")));
                        }
                    } catch (IOException e) {
                        log.error("Error to get dimensions", e);
                    }
                    return false;
                }

                return false;
            }

            return false;
        });

        this.suggesterBuilder.setSuggester(AlertExpressionParser.RULE_durationExpression, (inputs, expectedToken, suggestions) -> {
            // No suggestion for duration expression
            return false;
        });
    }

    private String getDataSource(List<? extends Token> inputs, int filterExpressionIndex) {
        // -1 to get the metric name
        // -1 to get the DOT
        // -1 to get the data source name
        int dataSourceIndex = filterExpressionIndex - 3;

        if (dataSourceIndex > 0) {
            return inputs.get(dataSourceIndex).getText();
        } else {
            return null;
        }
    }

    public Collection<Suggestion> suggest(String expression) {
        return suggesterBuilder.build().suggest(expression);
    }
}
