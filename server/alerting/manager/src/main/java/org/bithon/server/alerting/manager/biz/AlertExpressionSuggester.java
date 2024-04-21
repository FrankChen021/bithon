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

        this.suggesterBuilder.setSuggester(AlertExpressionParser.RULE_aggregatorExpression, (inputs, tokenHint, suggestions) -> {
            if (tokenHint.expectedTokenType == AlertExpressionParser.IDENTIFIER) {
                suggestions.add(new Suggestion(tokenHint.expectedTokenType, "sum"));
                suggestions.add(new Suggestion(tokenHint.expectedTokenType, "count"));
                suggestions.add(new Suggestion(tokenHint.expectedTokenType, "avg"));
                suggestions.add(new Suggestion(tokenHint.expectedTokenType, "max"));
                suggestions.add(new Suggestion(tokenHint.expectedTokenType, "min"));
            }
            return false;
        });

        // Suggest column names for GROUP BY expression
        this.suggesterBuilder.setSuggester(AlertExpressionParser.RULE_groupByExpression, (inputs, tokenHint, suggestions) -> {
            if (tokenHint.expectedTokenType != AlertExpressionParser.IDENTIFIER) {
                return false;
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
                          .forEach(col -> suggestions.add(new Suggestion(tokenHint.expectedTokenType, col.getName())));
                }
            }
            
            return false;
        });

        // Suggest data source names
        this.suggesterBuilder.setSuggester(AlertExpressionParser.RULE_dataSourceExpression, (inputs, tokenHint, suggestions) -> {
            dataSourceApi.getSchemaNames()
                         .forEach((value) -> suggestions.add(new Suggestion(tokenHint.expectedTokenType, value.getValue())));
            return false;
        });

        // Suggest metric names for the metric name expression
        this.suggesterBuilder.setSuggester(AlertExpressionParser.RULE_metricNameExpression, (inputs, tokenHint, suggestions) -> {
            if (tokenHint.expectedTokenType != AlertExpressionParser.IDENTIFIER) {
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
                      .forEach(col -> suggestions.add(new Suggestion(tokenHint.expectedTokenType, col.getName())));
            }

            return false;
        });

        this.suggesterBuilder.setSuggester(AlertExpressionParser.RULE_filterExpression, (inputs, tokenHint, suggestions) -> {
            if (tokenHint.expectedTokenType != AlertExpressionParser.IDENTIFIER) {
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
                          .forEach(col -> suggestions.add(new Suggestion(tokenHint.expectedTokenType, col.getName())));
                }
            }

            return false;
        });

        this.suggesterBuilder.setSuggester(AlertExpressionParser.RULE_literalExpression, (inputs, tokenHint, suggestions) -> {
            // No suggestion for literal expressions except the NULL
            //if (tokenHint.expectedTokenType == AlertExpressionParser.NULL_LITERAL
            //    && !inputs.isEmpty()
            //    && inputs.get(inputs.size() - 1).getText().equalsIgnoreCase("IS")
            //) {
            //    suggestions.add(new Suggestion(tokenHint.expectedTokenType, "NULL"));
            //}
            return false;
        });

        this.suggesterBuilder.setSuggester(AlertExpressionParser.RULE_durationExpression, (inputs, tokenHint, suggestions) -> {
            // No suggestion for duration expression
            return false;
        });
    }

    public Collection<Suggestion> suggest(String expression) {
        return suggesterBuilder.build().suggest(expression);
    }
}
