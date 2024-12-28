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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        return str != null && !str.isEmpty() && containsText(str);
    }

    public static boolean isEmpty(String v) {
        return v == null || v.trim().isEmpty();
    }

    public static boolean isBlank(String str) {
        return !hasText(str);
    }

    private static boolean containsText(CharSequence str) {
        for (int i = 0, len = str.length(); i < len; i++) {
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
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
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
     * For all character of toBeEscaped in the input, escape it with escapeCharacter.
     */
    public static String escape(String input, char escapeCharacter, char toBeEscaped) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        int inputLength = input.length();
        StringBuilder escaped = new StringBuilder(inputLength + 1);
        for (int i = 0; i < inputLength; i++) {
            char c = input.charAt(i);
            if (c == toBeEscaped) {
                escaped.append(escapeCharacter).append(c);
            } else {
                escaped.append(c);
            }
        }
        return escaped.toString();
    }

    public static String unEscape(String input, char escapeCharacter, char escaped) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        int inputLength = input.length();

        StringBuilder unEscaped = new StringBuilder(inputLength);
        for (int i = 0; i < inputLength; i++) {
            char c = input.charAt(i);

            if (c == escapeCharacter && i + 1 < inputLength) {
                char next = input.charAt(i + 1);
                if (next == escaped) {
                    unEscaped.append(next);
                    i++;
                    continue;
                }
            }

            unEscaped.append(c);
        }
        return unEscaped.toString();
    }

    public static Map<String, String> extractKeyValueParis(String kvPairs,
                                                           String pairSeparator,
                                                           String kvSeparator,
                                                           Map<String, String> map) {
        extractKeyValueParis(kvPairs, 0, pairSeparator, kvSeparator, map::put);
        return map;
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
     * @param kvStart       the position in the kvPairs to extract from
     * @param pairSeparator the separator of k-v pair
     * @param kvSeparator   the separator of key and value
     * @param kvConsumer    the callback processing for an extracted key-value pair
     */
    public static void extractKeyValueParis(String kvPairs,
                                            int kvStart,
                                            String pairSeparator,
                                            String kvSeparator,
                                            BiConsumer<String, String> kvConsumer) {
        if (StringUtils.isEmpty(kvPairs)) {
            return;
        }

        do {
            int kvEnd = kvPairs.indexOf(pairSeparator, kvStart);
            if (kvEnd == -1) {
                kvEnd = kvPairs.length();
            }

            if (kvEnd - kvStart > 0) {

                // Find kvSeparator in the range of [kvStart, kvEnd)
                int kvSeparatorIndex;
                for (kvSeparatorIndex = kvStart; kvSeparatorIndex < kvEnd; kvSeparatorIndex++) {
                    if (kvPairs.regionMatches(kvSeparatorIndex, kvSeparator, 0, kvSeparator.length())) {
                        kvConsumer.accept(getTrimedSubstring(kvPairs, kvStart, kvSeparatorIndex),
                                          getTrimedSubstring(kvPairs, kvSeparatorIndex + kvSeparator.length(), kvEnd));
                        break;
                    }
                }

                if (kvSeparatorIndex == kvEnd) {
                    // The kvSeparator is not found in the given range, treat the whole content as the key
                    kvConsumer.accept(getTrimedSubstring(kvPairs, kvStart, kvEnd), "");
                }
            } else {
                // Skip empty pair
            }

            kvStart = kvEnd + pairSeparator.length();
        } while (kvStart < kvPairs.length());
    }

    public static boolean isHexString(String text) {
        if (text == null) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c < '0' || c > '9') && (c < 'a' || c > 'f') && (c < 'A' || c > 'F')) {
                return false;
            }
        }
        return true;
    }

    /**
     * Split the input string by the separator and returns a list of separated trimmed strings.
     * NOTE: Empty string at the end of the list is not included in the result list
     */
    public static List<String> split(String str, String separator) {
        if (str == null || separator == null || separator.isEmpty()) {
            throw new IllegalArgumentException("Input string and separator must not be null or empty");
        }

        List<String> parts = new ArrayList<>();
        int start = 0;
        int separatorIndex;
        int separatorLength = separator.length();
        while ((separatorIndex = str.indexOf(separator, start)) != -1) {
            parts.add(getTrimedSubstring(str, start, separatorIndex));

            start = separatorIndex + separatorLength;
        }

        // if start == 0, the substring method returns itself
        if (start == 0) {
            parts.add(str);
        } else {
            String last = getTrimedSubstring(str, start, str.length());
            if (!last.isEmpty()) { // Skip the last empty string
                parts.add(last);
            }
        }

        return parts;
    }

    /**
     * @param start inclusive
     * @param end   exclusive
     */
    private static String getTrimedSubstring(String str, int start, int end) {
        // Trim the leading whitespaces
        while (start < end && Character.isWhitespace(str.charAt(start))) {
            start++;
        }

        // Trim the trailing whitespaces
        while (end > start && Character.isWhitespace(str.charAt(end - 1))) {
            end--;
        }

        return str.substring(start, end);
    }
}
