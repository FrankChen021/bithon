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

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/4/20 19:17
 */
public class TextBasedLexerAndParserFactory implements LexerAndParserFactory {

    final LexerGrammar lg;
    final Grammar g;

    public TextBasedLexerAndParserFactory(String grammarText) {
        try {
            lg = new LexerGrammar(grammarText);
            g = new Grammar(grammarText);
        } catch (org.antlr.runtime.RecognitionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Parser createParser(TokenStream tokenStream) {
        return g.createParserInterpreter(tokenStream);
    }

    @Override
    public Lexer createLexer(CharStream input) {
        return lg.createLexerInterpreter(input);
    }
}
