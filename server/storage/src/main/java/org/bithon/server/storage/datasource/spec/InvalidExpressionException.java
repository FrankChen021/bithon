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

package org.bithon.server.storage.datasource.spec;

import javax.validation.constraints.NotNull;
import java.util.Locale;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/11
 */
public class InvalidExpressionException extends RuntimeException {
    public InvalidExpressionException(@NotNull String expression, int line, int charPos, String parseExceptionMessage) {
        super(String.format(Locale.ENGLISH, "%s\n%s", parseExceptionMessage, formatMessage(expression, line, charPos)));
    }

    private static String formatMessage(String text, int line, int pos) {
        StringBuilder message = new StringBuilder(text.length());

        int lines = 0;
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            message.append(chars[i]);

            if (chars[i] == '\n' && ++lines == line) {
                while (pos-- > 0) {
                    message.append(' ');
                }
                message.append('^');
                message.append('\n');
            }
        }

        if (pos > 0) {
            message.append('\n');
            while (pos-- > 0) {
                message.append(' ');
            }
            message.append('^');
        }

        return message.toString();
    }
}
