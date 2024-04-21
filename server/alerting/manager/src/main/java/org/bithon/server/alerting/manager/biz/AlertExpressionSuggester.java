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

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bithon.server.alerting.common.parser.AlertExpressionLexer;
import org.bithon.server.alerting.common.parser.AlertExpressionParser;
import org.bithon.server.commons.autocomplete.AutoSuggesterBuilder;
import org.bithon.server.commons.autocomplete.CasePreference;
import org.bithon.server.commons.autocomplete.DefaultLexerAndParserFactory;
import org.bithon.server.commons.autocomplete.Suggestion;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.column.StringColumn;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/4/14 12:33
 */
@Component
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
                return false;
            }

            // Find the datasource token
            int i = inputs.size() - 1;
            while (i > 0 && inputs.get(i).getType() != AlertExpressionParser.LEFT_CURLY_BRACE) {
                --i;
            }

            // Now the 'i' points to the LEFT_CURLY_BRACE,
            // -1 to get the metric name
            // -1 to get the DOT
            // -1 to get the data source name
            i -= 3;

            if (i > 0) {
                String dataSource = inputs.get(i).getText();
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

        this.suggesterBuilder.setSuggester(AlertExpressionParser.RULE_literalExpression, (inputs, expectedToken, suggestions) -> {
            // No suggestion for literal expressions except the NULL
            //if (expectedToken.expectedTokenType == AlertExpressionParser.NULL_LITERAL
            //    && !inputs.isEmpty()
            //    && inputs.get(inputs.size() - 1).getText().equalsIgnoreCase("IS")
            //) {
            //    suggestions.add(Suggestion.of(expectedToken.expectedTokenType, "NULL"));
            //}
            return false;
        });

        this.suggesterBuilder.setSuggester(AlertExpressionParser.RULE_durationExpression, (inputs, expectedToken, suggestions) -> {
            // No suggestion for duration expression
            return false;
        });
    }

    public Collection<Suggestion> suggest(String expression) {
        return suggesterBuilder.build().suggest(expression);
    }
}
