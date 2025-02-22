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


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 31/1/24 10:09 am
 */
public class StringUtilsTest {

    @Test
    public void testBase16Encoding() {
        String id = "ea91433abf7d42a4b5afbbbeb8f71cec";
        String id2 = StringUtils.base16BytesToString(StringUtils.base16StringToBytes(id));
        Assertions.assertEquals(id, id2);
    }

    @Test
    public void testBase16Encoding_ByteAccessor() {
        String id = "ea91433abf7d42a4b5afbbbeb8f71cec";

        byte[] bytes = StringUtils.base16StringToBytes(id);

        String id2 = StringUtils.base16BytesToString((idx) -> bytes[idx], bytes.length);
        Assertions.assertEquals(id, id2);
    }

    @Test
    public void testCamelToSnake() {
        Assertions.assertNull(StringUtils.camelToSnake(null));
        Assertions.assertEquals("", StringUtils.camelToSnake(""));

        Assertions.assertEquals("print", StringUtils.camelToSnake("print"));
        Assertions.assertEquals("show_database", StringUtils.camelToSnake("showDatabase"));
        Assertions.assertEquals("show_database_and_table", StringUtils.camelToSnake("showDatabaseAndTable"));

        Assertions.assertEquals("print_i", StringUtils.camelToSnake("printI"));
        Assertions.assertEquals("print_io", StringUtils.camelToSnake("printIO"));
        Assertions.assertEquals("print_io_and", StringUtils.camelToSnake("printIOAnd"));
    }

    @Test
    public void testEscapeSingleQuote() {
        Assertions.assertEquals("\\'", StringUtils.escape("'", '\\', '\''));
        Assertions.assertEquals("\\'\\'", StringUtils.escape("''", '\\', '\''));

        Assertions.assertEquals("\\'a\\'", StringUtils.escape("'a'", '\\', '\''));
        Assertions.assertEquals("\\'a", StringUtils.escape("'a", '\\', '\''));
        Assertions.assertEquals("a\\'", StringUtils.escape("a'", '\\', '\''));
        Assertions.assertEquals("Frank\\'s", StringUtils.escape("Frank's", '\\', '\''));

        // Make sure existing escape character is not escaped
        Assertions.assertEquals("\\t", StringUtils.escape("\\t", '\\', '\''));

        // No need to escape already escaped input
        Assertions.assertEquals("\\\\'", StringUtils.escape("\\'", '\\', '\''));
        Assertions.assertEquals("b\\\\'", StringUtils.escape("b\\'", '\\', '\''));

        // Escape consecutive single quotes
        Assertions.assertEquals("\\\\'\\'", StringUtils.escape("\\''", '\\', '\''));

        // There are 4 '\'s in the input, and are all escaped,
        // But the single quote is not escaped, so it should be escaped
        Assertions.assertEquals("a\\\\\\\\\\'", StringUtils.escape("a\\\\\\\\'", '\\', '\''));
    }

    @Test
    public void testEscapePercentSign() {
        Assertions.assertEquals("\\%", StringUtils.escape("%", '\\', '%'));
        Assertions.assertEquals("\\%\\%", StringUtils.escape("%%", '\\', '%'));

        Assertions.assertEquals("\\%a", StringUtils.escape("%a", '\\', '%'));
        Assertions.assertEquals("a\\%", StringUtils.escape("a%", '\\', '%'));
        Assertions.assertEquals("\\%a\\%", StringUtils.escape("%a%", '\\', '%'));
        Assertions.assertEquals("Frank\\%s", StringUtils.escape("Frank%s", '\\', '%'));

        Assertions.assertEquals("\\\\%\\%", StringUtils.escape("\\%%", '\\', '%'));
    }

    @Test
    public void testUnEscape() {
        Assertions.assertEquals("%", StringUtils.unEscape("%", '\\', '%'));
        Assertions.assertEquals("%a", StringUtils.unEscape("%a", '\\', '%'));
        Assertions.assertEquals("a%", StringUtils.unEscape("a\\%", '\\', '%'));
        Assertions.assertEquals("\\a%", StringUtils.unEscape("\\a\\%", '\\', '%'));
    }

    @Test
    public void testIsHexString_ValidHex() {
        Assertions.assertTrue(StringUtils.isHexString("1a2b3c4d5e6f"));
        Assertions.assertTrue(StringUtils.isHexString("ABCDEF"));
        Assertions.assertTrue(StringUtils.isHexString("1234567890abcdef"));
    }

    @Test
    public void testIsHexString_InvalidHex() {
        Assertions.assertFalse(StringUtils.isHexString("1g2h3i4j5k6l"));
        Assertions.assertFalse(StringUtils.isHexString("XYZ"));
        Assertions.assertFalse(StringUtils.isHexString("12345G7890"));
    }

    @Test
    public void testIsHexString_EmptyString() {
        Assertions.assertTrue(StringUtils.isHexString(""));
    }

    @Test
    public void testIsHexString_NullString() {
        Assertions.assertFalse(StringUtils.isHexString(null));
    }

    @Test
    public void testIsHexString_SpecialCharacters() {
        Assertions.assertFalse(StringUtils.isHexString("!@#$%^&*()"));
        Assertions.assertFalse(StringUtils.isHexString("1234-5678"));
    }

    @Test
    public void testSplit_NormalCase() {
        String input = "a,b,c";
        String separator = ",";
        List<String> result = StringUtils.split(input, separator);
        Assertions.assertArrayEquals(new String[]{"a", "b", "c"}, result.toArray());
    }

    @Test
    public void testSplit_NoSeparator() {
        String input = "abc";
        String separator = ",";
        List<String> result = StringUtils.split(input, separator);
        Assertions.assertArrayEquals(new String[]{"abc"}, result.toArray());
    }

    @Test
    public void testSplit_SeparatorAtBeginning() {
        String input = ",a,b,c";
        String separator = ",";
        List<String> result = StringUtils.split(input, separator);
        Assertions.assertArrayEquals(new String[]{"", "a", "b", "c"}, result.toArray());
    }

    @Test
    public void testSplit_SeparatorAtEnd() {
        String input = "a,b,c,";
        String separator = ",";
        List<String> result = StringUtils.split(input, separator);
        Assertions.assertArrayEquals(new String[]{"a", "b", "c"}, result.toArray());
    }

    @Test
    public void testSplit_MultipleConsecutiveSeparators() {
        String input = "a,,b,c";
        String separator = ",";
        List<String> result = StringUtils.split(input, separator);
        Assertions.assertArrayEquals(new String[]{"a", "", "b", "c"}, result.toArray());
    }

    @Test
    public void testSplit_Trimmed() {
        String input = "a, , b , c ";
        String separator = ",";
        List<String> result = StringUtils.split(input, separator);
        Assertions.assertArrayEquals(new String[]{"a", "", "b", "c"}, result.toArray());
    }

    @Test
    public void testSplit_NullInput() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> StringUtils.split(null, ","));
    }

    @Test
    public void testSplit_EmptySeparator() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> StringUtils.split("a,b,c", ""));
    }

    @Test
    public void testSplit_NullSeparator() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> StringUtils.split("a,b,c", null));
    }

    @Test
    public void testExtractKeyValueParis_NormalCase() {
        String input = "key1=value1&key2=value2&";
        Map<String, String> result = StringUtils.extractKeyValueParis(input, "&", "=", new LinkedHashMap<>());
        Map<String, String> expected = new HashMap<>();
        expected.put("key1", "value1");
        expected.put("key2", "value2");
        Assertions.assertEquals(expected, result);
    }


    @Test
    public void testExtractKeyValueParis_NormalCase_2() {
        String input = "key1=value1; key2=value2; ";
        Map<String, String> result = StringUtils.extractKeyValueParis(input, "; ", "=", new LinkedHashMap<>());
        Map<String, String> expected = new HashMap<>();
        expected.put("key1", "value1");
        expected.put("key2", "value2");
        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testExtractKeyValueParis_EmptyInput() {
        String input = "";
        Map<String, String> result = StringUtils.extractKeyValueParis(input, "&", "=", new LinkedHashMap<>());
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testExtractKeyValueParis_NullInput() {
        Map<String, String> result = StringUtils.extractKeyValueParis(null, "&", "=", new LinkedHashMap<>());
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testExtractKeyValueParis_MissingKeyOrValue() {
        String input = "key1=&=value2&key3=value3";
        Map<String, String> result = StringUtils.extractKeyValueParis(input, "&", "=", new LinkedHashMap<>());
        Map<String, String> expected = new HashMap<>();
        expected.put("key1", "");
        expected.put("", "value2");
        expected.put("key3", "value3");
        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testExtractKeyValueParis_SpecialCharacters() {
        String input = "key1=val!@#ue1&key2=val$%^ue2";
        Map<String, String> result = StringUtils.extractKeyValueParis(input, "&", "=", new LinkedHashMap<>());
        Map<String, String> expected = new HashMap<>();
        expected.put("key1", "val!@#ue1");
        expected.put("key2", "val$%^ue2");
        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testExtractKeyValueParis_TrimKeyAndValue() {
        String input = "key1 = value1 & key2 = value2";
        Map<String, String> result = StringUtils.extractKeyValueParis(input, "&", "=", new LinkedHashMap<>());
        Map<String, String> expected = new HashMap<>();
        expected.put("key1", "value1");
        expected.put("key2", "value2");
        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testExtractKeyValueParis_EmptyPair() {
        String input = "&a&&key1=value1&&&key2==value2&&a&";
        Map<String, String> result = StringUtils.extractKeyValueParis(input, "&", "=", new LinkedHashMap<>());
        Map<String, String> expected = new HashMap<>();
        expected.put("key1", "value1");

        // Whether value has leading '=' depends on how we define such behavior
        // Currently the content after the first kv-separator (in this case it's the '=') is the value
        expected.put("key2", "=value2");

        expected.put("a", "");
        Assertions.assertEquals(expected, result);
    }
}
