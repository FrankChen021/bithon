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
import org.bithon.component.commons.utils.StringUtils;

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

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {

        Token token = (Token) offendingSymbol;
        throw new InvalidExpressionException(format(expression,
                                                    token,
                                                    line,
                                                    charPositionInLine,
                                                    msg));
    }

    private String format(String expression,
                          Token offendingToken,
                          int line,
                          int charPositionInLine,
                          String error) {
        StringBuilder messages = new StringBuilder(128);
        messages.append(StringUtils.format("Invalid expression at line %d:%d %s\n", line, charPositionInLine, error));

        String[] lines = expression.split("\n");
        String errorLine = lines[line - 1];

        messages.append(errorLine);
        messages.append('\n');
        messages.append(" ".repeat(Math.max(0, charPositionInLine)));

        int indicatorLength = 1;
        if (offendingToken != null) {
            int start = offendingToken.getStartIndex();
            int stop = offendingToken.getStopIndex();
            indicatorLength = Math.max(1, stop - start + 1);
        }
        messages.append("^".repeat(indicatorLength));

        return messages.toString();
    }
}
