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

import org.bithon.component.commons.exception.HttpMappableException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author frank.chen021@outlook.com
 * @date 13/11/21 12:42 pm
 */
public class StringUtils {

    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();
    private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();

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

    public static String getOrEmpty(String v) {
        return v == null ? "" : v;
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

    public static String base64BytesToString(byte[] input) {
        return BASE64_ENCODER.encodeToString(input);
    }

    public static byte[] base64StringToBytes(String input) {
        return BASE64_DECODER.decode(input);
    }

    public static String base16BytesToString(byte[] input) {
        int index = 0;
        char[] buf = new char[input.length * 2];
        for (byte b : input) {
            char upper = (char) ((b & 0xF0) >> 4);
            buf[index++] = (char) (upper >= 10 ? 'a' + upper - 10 : '0' + upper);

            char lower = (char) (b & 0x0F);
            buf[index++] = (char) (lower >= 10 ? 'a' + lower - 10 : '0' + lower);
        }
        return new String(buf);
    }

    public static String base16BytesToString(Function<Integer, Byte> byteAccessor, int length) {
        int index = 0;
        char[] buf = new char[length * 2];
        for (int i = 0; i < length; i++) {
            byte b = byteAccessor.apply(i);

            char upper = (char) ((b & 0xF0) >> 4);
            buf[index++] = (char) (upper >= 10 ? 'a' + upper - 10 : '0' + upper);

            char lower = (char) (b & 0x0F);
            buf[index++] = (char) (lower >= 10 ? 'a' + lower - 10 : '0' + lower);
        }
        return new String(buf);
    }

    public static byte[] base16StringToBytes(String input) {
        byte[] bytes = new byte[input.length() / 2];
        int index = 0;
        for (int i = 0, size = input.length(); i < size; i += 2) {
            char higher = input.charAt(i);
            higher = (char) (higher > '9' ? higher - 'a' + 10 : higher - '0');

            char lower = input.charAt(i + 1);
            lower = (char) (lower > '9' ? lower - 'a' + 10 : lower - '0');

            bytes[index++] = (byte) ((higher << 4) | lower);
        }
        return bytes;
    }

    /**
     * Converts a camel style identifier name into snake style.
     * Examples:
     * print - print
     * printIO - print_io
     * printIOStat - print_io_stat
     * printName - print_name
     */
    public static String camelToSnake(String camelCase) {
        if (camelCase == null) {
            return null;
        }

        // The number of consecutive upper cases right before current character
        int consecutiveUpperCases = 0;

        StringBuilder snakeCase = new StringBuilder(camelCase.length());
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0 && consecutiveUpperCases == 0) {
                    snakeCase.append('_');
                }
                snakeCase.append(Character.toLowerCase(c));

                consecutiveUpperCases++;
            } else {
                if (consecutiveUpperCases > 1) {
                    // For example, for given input: printIOAnd
                    // when comes the 'n' character after the 'A',
                    // the 'snakeCase' variable holds: print_ioa, and the variable consecutiveUpperCases is 2,
                    // We need to insert an underscore as a separator to turn it into print_io_a
                    snakeCase.insert(snakeCase.length() - 1, '_');
                }
                snakeCase.append(c);

                consecutiveUpperCases = 0;
            }
        }

        return snakeCase.toString();
    }

    /**
     * @param input      The input string that needs to be escaped.
     *                   If the input has escaped character by using leading \ character,
     *                   the escaped character will not be escaped again.
     * @param escapeChar The character that is used to escape the single quote. Like ' or \
     */
    public static String escapeSingleQuoteIfNecessary(String input, char escapeChar) {
        int i = input.indexOf('\'');
        if (i < 0) {
            // If no single quote found, no escape is needed
            return input;
        }

        StringBuilder escaped = new StringBuilder(input.length() + 1);
        {
            // Processing from non-escape character
            while (i > 0 && input.charAt(--i) == '\\') {
            }
            if (i > 0) {
                // Before the slash character, there are characters
                escaped.append(input, 0, i);
            }

            for (int size = input.length(); i < size; i++) {
                char c = input.charAt(i);
                if (c == '\\') {
                    if (i + 1 == size) {
                        throw new HttpMappableException(400,
                                                        StringUtils.format("The string literal[%s] has ill-format escaping at the end of the string."));
                    }
                    char next = input.charAt(++i);
                    if (next == '\'') {
                        escaped.append(escapeChar);
                    } else {
                        escaped.append('\\');
                    }
                    escaped.append(next);
                } else if (c == '\'') {
                    // Escape the single quote
                    escaped.append(escapeChar);
                    escaped.append(c);
                } else {
                    escaped.append(c);
                }
            }
        }
        return escaped.toString();
    }

    public static void extractKeyValueParis(String kvPairs,
                                            String pairSeparator,
                                            String kvSeparator,
                                            BiConsumer<String, String> pairConsumer) {
        extractKeyValueParis(kvPairs, 0, pairSeparator, kvSeparator, pairConsumer);
    }

    /**
     * Extract key-values from a given string.For example, for below input:
     * kvPairs: a=b&c=d
     * pairSeparator: &
     * kvSeparator: =
     * <p>
     * The result would be:
     * (a,b)
     * (c,d)
     *
     * @param kvPairs       the input string
     * @param startingFrom  the position of the kvPairs to extract from
     * @param pairSeparator the separator of k-v pair
     * @param kvSeparator   the separator of key and value
     * @param kvConsumer    the callback processing for an extracted key-value pair
     */
    public static void extractKeyValueParis(String kvPairs,
                                            int startingFrom,
                                            String pairSeparator,
                                            String kvSeparator,
                                            BiConsumer<String, String> kvConsumer) {
        if (StringUtils.isEmpty(kvPairs)) {
            return;
        }

        int tokenStart = startingFrom;
        int tokenEnd;
        do {
            tokenEnd = kvPairs.indexOf(kvSeparator, tokenStart);
            if (tokenEnd > tokenStart) {
                // Find the parameter name
                String name = kvPairs.substring(tokenStart, tokenEnd);

                // +1 to skip the kvSeparator
                tokenStart = tokenEnd + 1;

                // Find the parameter value
                tokenEnd = kvPairs.indexOf(pairSeparator, tokenStart);
                if (tokenEnd == -1) { // If there's no '&' found, the whole is the value
                    kvConsumer.accept(name, kvPairs.substring(tokenStart));
                } else { // If there's a pair separator found, get the substring as value
                    kvConsumer.accept(name, kvPairs.substring(tokenStart, tokenEnd));
                }

                tokenStart = tokenEnd + 1;
            } else if (tokenEnd == tokenStart) {
                // extra '=' found, e.g: queryString equals to kvSeparator
                tokenStart = tokenEnd + 1;
            } else {
                // Not found '=', tokenEnd is -1,
            }
        } while (tokenEnd != -1);
    }
}
