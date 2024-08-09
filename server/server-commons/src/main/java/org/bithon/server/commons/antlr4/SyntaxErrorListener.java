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

package org.bithon.server.commons.antlr4;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/7/20 17:27
 */
public class SyntaxErrorListener extends BaseErrorListener {
    public static SyntaxErrorListener of(String expression) {
        return new SyntaxErrorListener(expression);
    }

    private final String expression;

    private SyntaxErrorListener(String expression) {
        this.expression = expression;
    }

    /**
     * For such an expression input: 12_
     * It triggers the token recognition error, and the offending symbol is NULL.
     */
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {

        Token token = (Token) offendingSymbol;
        throw InvalidExpressionException.format(expression,
                                                token == null ? null : token.getStartIndex(),
                                                token == null ? null : token.getStopIndex(),
                                                line,
                                                charPositionInLine,
                                                msg);
    }
}
