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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Frank Chen
 * @date 13/11/21 12:42 pm
 */
public class StringUtils {

    public static String format(String message, Object... formatArgs) {
        return String.format(Locale.ENGLISH, message, formatArgs);
    }

    public static String toUpper(String s) {
        return s == null ? null : s.toUpperCase(Locale.ENGLISH);
    }

    public static String toLower(String s) {
        return s == null ? null : s.toLowerCase(Locale.ENGLISH);
    }

    public static boolean hasText(String str) {
        return (str != null && !str.isEmpty() && containsText(str));
    }

    /**
     * @param timestamp timestamp in millisecond
     */
    public static String formatDateTime(String pattern, long timestamp) {
        return new SimpleDateFormat(pattern, Locale.ENGLISH).format(new Date(timestamp));
    }

    private static boolean containsText(CharSequence str) {
        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
