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

package org.bithon.component.commons.utils;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/1/17 23:39
 */
public class SqlParser {
    public static String parseSqlType(String sql) {
        return parseSqlType(sql,
                            // 10 is a reasonable size for SQL type such as SELECT/INSERT
                            10);
    }

    private enum State {
        NORMAL,
        SINGLE_LINE_COMMENT,
        MULTI_LINE_COMMENT
    }

    private static String parseSqlType(String sql, int maxSizeToLookFor) {
        StringBuilder keyword = new StringBuilder();

        State state = State.NORMAL;
        for (int i = 0, size = sql.length(); i < size; i++) {
            char c = sql.charAt(i);

            switch (state) {
                case NORMAL:
                    if (c == '-' && i + 1 < size && sql.charAt(i + 1) == '-') {
                        state = State.SINGLE_LINE_COMMENT;
                        i++;
                    } else if (c == '/' && i + 1 < size && sql.charAt(i + 1) == '*') {
                        state = State.MULTI_LINE_COMMENT;
                        i++;
                    } else if (Character.isWhitespace(c)) {
                        if (keyword.length() > 0) {
                            return keyword.toString();
                        }
                    } else {
                        keyword.append(Character.toUpperCase(c));
                        if (keyword.length() >= maxSizeToLookFor) {
                            return keyword.toString();
                        }
                    }
                    break;

                case SINGLE_LINE_COMMENT:
                    if (c == '\n') {
                        state = State.NORMAL;
                    }
                    break;

                case MULTI_LINE_COMMENT:
                    if (c == '*' && i + 1 < sql.length() && sql.charAt(i + 1) == '/') {
                        state = State.NORMAL;
                        i++;
                    }
                    break;
            }
        }

        return keyword.toString();
    }
}
