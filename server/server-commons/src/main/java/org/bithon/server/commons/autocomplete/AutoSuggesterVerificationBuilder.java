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

package org.bithon.server.commons.autocomplete;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.bithon.component.commons.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * @author frank.chen021@outlook.com
 * @date 15/4/24 9:48 pm
 */
public class AutoSuggesterVerificationBuilder {

    private LexerAndParserFactory lexerAndParserFactory;
    private CasePreference casePreference;
    private Collection<Suggestion> suggestions;

    public static AutoSuggesterVerificationBuilder builder() {
        return new AutoSuggesterVerificationBuilder();
    }

    public AutoSuggesterVerificationBuilder givenGrammar(String... grammarLines) {
        this.lexerAndParserFactory = loadGrammar(grammarLines);
        printGrammarAtnIfNeeded();
        return this;
    }

    public AutoSuggesterVerificationBuilder withCasePreference(CasePreference casePreference) {
        this.casePreference = casePreference;
        return this;
    }

    /*
     * Used for testing with generated grammars, e.g., for checking out reported issues, before coming up with a more
     * focused test
     */
    public AutoSuggesterVerificationBuilder givenGrammar(Class<? extends Lexer> lexerClass, Class<? extends Parser> parserClass) {
        this.lexerAndParserFactory = new DefaultLexerAndParserFactory(lexerClass, parserClass);
        printGrammarAtnIfNeeded();
        return this;
    }

    private void printGrammarAtnIfNeeded() {
        Logger logger = LoggerFactory.getLogger(this.getClass());
//        if (!logger.isDebugEnabled()) {
//            return;
//        }
        Lexer lexer = this.lexerAndParserFactory.createLexer(null);
        Parser parser = this.lexerAndParserFactory.createParser(null);
        String header = "\n===========  PARSER ATN  ====================\n";
        String middle = "===========  LEXER ATN   ====================\n";
        String footer = "===========  END OF ATN  ====================";
        String parserAtn = AdaptiveTransitionNetworkFormatter.format(parser);
        String lexerAtn = AdaptiveTransitionNetworkFormatter.format(lexer);
        System.out.println(header + parserAtn + middle + lexerAtn + footer);
    }

    public AutoSuggesterVerificationBuilder whenInput(String input) {
        this.suggestions = AutoSuggesterBuilder.builder()
                                               .factory(this.lexerAndParserFactory)
                                               .casePreference(this.casePreference)
                                               .build()
                                               .suggest(input);
        return this;
    }

    public void thenExpect(String... expectedCompletions) {
        for (String expected : expectedCompletions) {
            if (this.suggestions.stream().noneMatch((s) -> s.getText().equals(expected))) {
                throw new RuntimeException(StringUtils.format("Actual suggestion is [%s], Expected: [%s]", this.suggestions, expected));
            }
        }
    }

    private LexerAndParserFactory loadGrammar(String... grammarlines) {
        String firstLine = "grammar test;\n";
        String grammarText = firstLine + String.join(";\n", grammarlines) + ";\n";
        return new TextBasedLexerAndParserFactory(grammarText);
    }
}

