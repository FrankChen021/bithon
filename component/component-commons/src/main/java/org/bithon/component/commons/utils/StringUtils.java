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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

    public static boolean isEmpty(String v) {
        return v == null || v.trim().isEmpty();
    }

    public static boolean isBlank(String str) {
        return !hasText(str);
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

    public static String getSimpleClassName(String qualifiedClassName) {
        int dot = qualifiedClassName.lastIndexOf('.');
        return dot == -1 ? qualifiedClassName : qualifiedClassName.substring(dot + 1);
    }

    public static String from(InputStream inputStream) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int length;
        byte[] buffer = new byte[1024];
        while ((length = inputStream.read(buffer, 0, buffer.length)) != -1) {
            bos.write(buffer, 0, length);
        }
        bos.flush();

        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }
}
