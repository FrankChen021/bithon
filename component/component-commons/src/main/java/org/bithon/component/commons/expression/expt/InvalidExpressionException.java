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

package org.bithon.component.commons.expression.expt;

import org.bithon.component.commons.utils.StringUtils;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/8 6:18 下午
 */
public class InvalidExpressionException extends RuntimeException {

    public InvalidExpressionException(String message) {
        super(message);
    }

    public InvalidExpressionException(String format, Object... args) {
        super(StringUtils.format(format, args));
    }

    public static InvalidExpressionException format(String expression,
                                                    Integer tokenStart,
                                                    Integer tokenEnd,
                                                    int line,
                                                    int charPositionInLine,
                                                    String error) {
        StringBuilder messages = new StringBuilder(128);
        messages.append(StringUtils.format("Invalid expression at line %d:%d %s\n", line, charPositionInLine, error));

        // Add the error line
        String[] lines = expression.split("\n");
        String errorLine = lines[line - 1];
        messages.append(errorLine);

        // Add an indicator to the position where the error occurs
        messages.append('\n');
        for (int i = 0; i < charPositionInLine; i++) {
            messages.append(' ');
        }
        int indicatorLength = (tokenStart != null && tokenEnd != null) ? tokenEnd - tokenStart + 1 : 1;
        for (int i = 0; i < indicatorLength; i++) {
            messages.append('^');
        }

        return new InvalidExpressionException(messages.toString());
    }
}
