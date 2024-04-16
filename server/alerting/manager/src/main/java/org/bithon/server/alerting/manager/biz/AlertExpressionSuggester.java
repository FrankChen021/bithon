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
import org.bithon.server.commons.autocomplete.AutoSuggester;
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
    private final AutoSuggester suggester;

    public AlertExpressionSuggester(IDataSourceApi dataSourceApi) {
        this.dataSourceApi = dataSourceApi;

        DefaultLexerAndParserFactory factory = new DefaultLexerAndParserFactory(
            AlertExpressionLexer.class,
            AlertExpressionParser.class
        );
        this.suggester = new AutoSuggester(factory);
        this.suggester.setCasePreference(CasePreference.UPPER);

        this.suggester.addSuggester(AlertExpressionParser.RULE_aggregatorExpression, (inputs, grammarRule, suggestions) -> {
            if (grammarRule.nextTokenType == AlertExpressionParser.IDENTIFIER) {
                suggestions.add(new Suggestion(grammarRule.nextTokenType, "sum"));
                suggestions.add(new Suggestion(grammarRule.nextTokenType, "count"));
                suggestions.add(new Suggestion(grammarRule.nextTokenType, "avg"));
                suggestions.add(new Suggestion(grammarRule.nextTokenType, "max"));
                suggestions.add(new Suggestion(grammarRule.nextTokenType, "min"));
            }
            return false;
        });

        this.suggester.addSuggester(AlertExpressionParser.RULE_groupByExpression, (inputs, grammarRule, suggestions) -> {
            // At this stage, the data source is not known, it's not able to suggest for IDENTIFIER
            return grammarRule.nextTokenType != AlertExpressionParser.IDENTIFIER;
        });

        this.suggester.addSuggester(AlertExpressionParser.RULE_dataSourceExpression, (inputs, grammarRule, suggestions) -> {
            dataSourceApi.getSchemas()
                         .forEach((schemaName, schema) -> suggestions.add(new Suggestion(grammarRule.nextTokenType, schemaName)));
            return false;
        });

        this.suggester.addSuggester(AlertExpressionParser.RULE_metricNameExpression, (inputs, grammarRule, suggestions) -> {
            if (grammarRule.nextTokenType != AlertExpressionParser.IDENTIFIER) {
                return false;
            }

            // -1 is the DOT
            String dataSource = inputs.get(inputs.size() - 2).getText();
            ISchema schema = dataSourceApi.getSchemaByName(dataSource);
            if (schema != null) {
                schema.getColumns()
                      .stream()
                      .filter((col) -> !(col instanceof StringColumn))
                      .forEach(col -> suggestions.add(new Suggestion(grammarRule.nextTokenType, col.getName())));
            }

            return false;
        });

        this.suggester.addSuggester(AlertExpressionParser.RULE_filterExpression, (inputs, grammarRule, suggestions) -> {
            if (grammarRule.nextTokenType != AlertExpressionParser.IDENTIFIER) {
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
                          .forEach(col -> suggestions.add(new Suggestion(grammarRule.nextTokenType, col.getName())));
                }
            }

            return false;
        });

        this.suggester.addSuggester(AlertExpressionParser.RULE_literalExpression, (inputs, grammarRule, suggestions) -> {
            // No suggestion for literal expressions
            return false;
        });

        this.suggester.addSuggester(AlertExpressionParser.RULE_durationExpression, (inputs, grammarRule, suggestions) -> {
            // No suggestion for duration expression
            return false;
        });
    }

    public Collection<Suggestion> suggest(String expression) {
        return suggester.suggest(expression);
    }
}
