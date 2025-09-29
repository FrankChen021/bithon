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

package org.bithon.agent.plugin.jdbc.common;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/2/3 20:11
 */
public class SqlTypeParser {

    private enum State {
        INITIAL,
        SINGLE_LINE_COMMENT,
        MULTI_LINE_COMMENT,
        STRING_LITERAL,
        READING_TYPE
    }

    public static String parse(String sql) {
        StringBuilder sqlType = new StringBuilder();
        int length = sql.length();
        State state = State.INITIAL;

        for (int i = 0; i < length; i++) {
            char c = sql.charAt(i);

            switch (state) {
                case INITIAL:
                    if (c == '-' && i + 1 < length && sql.charAt(i + 1) == '-') {
                        state = State.SINGLE_LINE_COMMENT;
                        i++;
                    } else if (c == '/' && i + 1 < length && sql.charAt(i + 1) == '*') {
                        state = State.MULTI_LINE_COMMENT;
                        i++;
                    } else if (c == '\'') {
                        state = State.STRING_LITERAL;
                    } else if (isUpper(c)) {
                        state = State.READING_TYPE;
                        sqlType.append(c);
                    } else if (isLower(c)) {
                        state = State.READING_TYPE;
                        // Convert to upper case
                        sqlType.append((char) (c - 32));
                    }
                    break;

                case SINGLE_LINE_COMMENT:
                    if (c == '\n') {
                        state = State.INITIAL;
                    }
                    break;

                case MULTI_LINE_COMMENT:
                    if (c == '*' && i + 1 < length && sql.charAt(i + 1) == '/') {
                        state = State.INITIAL;
                        i++;
                    }
                    break;

                case STRING_LITERAL:
                    if (c == '\'') {
                        state = State.INITIAL;
                    }
                    break;

                case READING_TYPE:
                    if (sqlType.length() < 10) {
                        if (isLower(c)) {
                            // Convert to upper case
                            sqlType.append((char) (c - 32));
                        } else if (isUpper(c)) {
                            sqlType.append(c);
                        } else {
                            return sqlType.toString();
                        }
                    } else {
                        return sqlType.toString();
                    }
                    break;
            }
        }

        return sqlType.toString();
    }

    private static boolean isLower(char c) {
        return (c >= 'a' && c <= 'z');
    }

    private static boolean isUpper(char c) {
        return (c >= 'A' && c <= 'Z');
    }
}
