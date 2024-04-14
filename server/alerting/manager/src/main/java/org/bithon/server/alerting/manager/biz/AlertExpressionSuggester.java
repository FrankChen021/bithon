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
        this.suggester.addSuggester((inputs, grammarRule, suggestions) -> {
            if (grammarRule.ruleIndex == AlertExpressionParser.RULE_literalExpression) {
                // No suggestion for literal expressions
                return false;
            }
            if(grammarRule.ruleIndex == AlertExpressionParser.RULE_durationExpression) {
                // No suggestion for duration expression
                return false;
            }

            if (grammarRule.nextTokenType == AlertExpressionParser.IDENTIFIER) {
                if (grammarRule.ruleIndex == AlertExpressionParser.RULE_dataSourceExpression) {
                    dataSourceApi.getSchemas()
                                 .forEach((schemaName, schema) -> {
                                     suggestions.add(new Suggestion(grammarRule.nextTokenType, schemaName));
                                 });
                    return false;
                }

                if (grammarRule.ruleIndex == AlertExpressionParser.RULE_metricNameExpression) {
                    String dataSource = inputs.get(inputs.size() - 2).getText();
                    ISchema schema = dataSourceApi.getSchemaByName(dataSource);
                    if (schema != null) {
                        schema.getColumns()
                              .stream()
                              .filter((col) -> !(col instanceof StringColumn))
                              .forEach(col -> {
                                  suggestions.add(new Suggestion(grammarRule.nextTokenType, col.getName()));
                              });
                    }
                    return false;
                }

                if (grammarRule.ruleIndex == AlertExpressionParser.RULE_filterExpression) {
                    suggestions.add(new Suggestion(grammarRule.nextTokenType, "appName"));
                    suggestions.add(new Suggestion(grammarRule.nextTokenType, "instanceName"));
                    return false;
                }

                suggestions.add(new Suggestion(grammarRule.nextTokenType, "app"));
                suggestions.add(new Suggestion(grammarRule.nextTokenType, "service"));
                suggestions.add(new Suggestion(grammarRule.nextTokenType, "host"));

                // No need to call the next suggester
                return false;
            }
            return true;
        });
    }

    public Collection<Suggestion> suggest(String expression) {
        return suggester.suggest(expression);
    }
}
