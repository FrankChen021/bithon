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

package org.bithon.server.storage.common.expression;

import org.antlr.v4.runtime.Token;
import org.bithon.component.commons.utils.StringUtils;

import java.util.Locale;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/11
 */
public class InvalidExpressionException extends RuntimeException {
    public InvalidExpressionException(String expression,
                                      Object offendingSymbol,
                                      int line,
                                      int charPositionInLine,
                                      String msg) {
        super(format(expression, (Token) offendingSymbol, line, charPositionInLine, msg));
    }

    public InvalidExpressionException(String format, Object... args) {
        super(String.format(Locale.ENGLISH, format, args));
    }

    public static String format(String expression,
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
        for (int i = 0; i < charPositionInLine; i++) {
            messages.append(' ');
        }

        int indicatorLength = 1;
        if (offendingToken != null) {
            int start = offendingToken.getStartIndex();
            int stop = offendingToken.getStopIndex();
            indicatorLength = Math.max(1, stop - start + 1);
        }
        for (int i = 0; i < indicatorLength; i++) {
            messages.append('^');
        }

        return messages.toString();
    }
}
