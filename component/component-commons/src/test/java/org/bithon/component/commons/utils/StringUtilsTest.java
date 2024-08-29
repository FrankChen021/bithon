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

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author Frank Chen
 * @date 31/1/24 10:09 am
 */
public class StringUtilsTest {

    @Test
    public void testBase16Encoding() {
        String id = "ea91433abf7d42a4b5afbbbeb8f71cec";
        String id2 = StringUtils.base16BytesToString(StringUtils.base16StringToBytes(id));
        Assert.assertEquals(id, id2);
    }

    @Test
    public void testBase16Encoding_ByteAccessor() {
        String id = "ea91433abf7d42a4b5afbbbeb8f71cec";

        byte[] bytes = StringUtils.base16StringToBytes(id);

        String id2 = StringUtils.base16BytesToString((idx) -> bytes[idx], bytes.length);
        Assert.assertEquals(id, id2);
    }

    @Test
    public void testCamelToSnake() {
        Assert.assertNull(StringUtils.camelToSnake(null));
        Assert.assertEquals("", StringUtils.camelToSnake(""));

        Assert.assertEquals("print", StringUtils.camelToSnake("print"));
        Assert.assertEquals("show_database", StringUtils.camelToSnake("showDatabase"));
        Assert.assertEquals("show_database_and_table", StringUtils.camelToSnake("showDatabaseAndTable"));

        Assert.assertEquals("print_i", StringUtils.camelToSnake("printI"));
        Assert.assertEquals("print_io", StringUtils.camelToSnake("printIO"));
        Assert.assertEquals("print_io_and", StringUtils.camelToSnake("printIOAnd"));
    }

    @Test
    public void testEscapeSingleQuote() {
        Assert.assertEquals("\\'", StringUtils.escapeSingleQuoteIfNecessary("'", '\\'));
        Assert.assertEquals("\\'a", StringUtils.escapeSingleQuoteIfNecessary("'a", '\\'));
        Assert.assertEquals("a\\'", StringUtils.escapeSingleQuoteIfNecessary("a'", '\\'));
        Assert.assertEquals("\\'\\'", StringUtils.escapeSingleQuoteIfNecessary("''", '\\'));
        Assert.assertEquals("Frank\\'s", StringUtils.escapeSingleQuoteIfNecessary("Frank's", '\\'));
        Assert.assertEquals("Frank\\'s", StringUtils.escapeSingleQuoteIfNecessary("Frank\\'s", '\\'));
        Assert.assertEquals("\\t", StringUtils.escapeSingleQuoteIfNecessary("\\t", '\\'));
        Assert.assertEquals("\\'", StringUtils.escapeSingleQuoteIfNecessary("\\'", '\\'));
        Assert.assertEquals("b\\'", StringUtils.escapeSingleQuoteIfNecessary("b\\'", '\\'));
        Assert.assertEquals("\\'\\'", StringUtils.escapeSingleQuoteIfNecessary("\\''", '\\'));
        Assert.assertEquals("a\\\\\\\\\\'", StringUtils.escapeSingleQuoteIfNecessary("a\\\\\\\\'", '\\'));
    }

    @Test
    public void testIsHexString_ValidHex() {
        Assert.assertTrue(StringUtils.isHexString("1a2b3c4d5e6f"));
        Assert.assertTrue(StringUtils.isHexString("ABCDEF"));
        Assert.assertTrue(StringUtils.isHexString("1234567890abcdef"));
    }

    @Test
    public void testIsHexString_InvalidHex() {
        Assert.assertFalse(StringUtils.isHexString("1g2h3i4j5k6l"));
        Assert.assertFalse(StringUtils.isHexString("XYZ"));
        Assert.assertFalse(StringUtils.isHexString("12345G7890"));
    }

    @Test
    public void testIsHexString_EmptyString() {
        Assert.assertTrue(StringUtils.isHexString(""));
    }

    @Test
    public void testIsHexString_NullString() {
        Assert.assertFalse(StringUtils.isHexString(null));
    }

    @Test
    public void testIsHexString_SpecialCharacters() {
        Assert.assertFalse(StringUtils.isHexString("!@#$%^&*()"));
        Assert.assertFalse(StringUtils.isHexString("1234-5678"));
    }

    @Test
    public void testSplit_NormalCase() {
        String input = "a,b,c";
        String separator = ",";
        List<String> result = StringUtils.split(input, separator);
        Assert.assertArrayEquals(new String[]{"a", "b", "c"}, result.toArray());
    }

    @Test
    public void testSplit_NoSeparator() {
        String input = "abc";
        String separator = ",";
        List<String> result = StringUtils.split(input, separator);
        Assert.assertArrayEquals(new String[]{"abc"}, result.toArray());
    }

    @Test
    public void testSplit_SeparatorAtBeginning() {
        String input = ",a,b,c";
        String separator = ",";
        List<String> result = StringUtils.split(input, separator);
        Assert.assertArrayEquals(new String[]{"", "a", "b", "c"}, result.toArray());
    }

    @Test
    public void testSplit_SeparatorAtEnd() {
        String input = "a,b,c,";
        String separator = ",";
        List<String> result = StringUtils.split(input, separator);
        Assert.assertArrayEquals(new String[]{"a", "b", "c"}, result.toArray());
    }

    @Test
    public void testSplit_MultipleConsecutiveSeparators() {
        String input = "a,,b,c";
        String separator = ",";
        List<String> result = StringUtils.split(input, separator);
        Assert.assertArrayEquals(new String[]{"a", "", "b", "c"}, result.toArray());
    }

    @Test
    public void testSplit_Trimmed() {
        String input = "a, , b , c ";
        String separator = ",";
        List<String> result = StringUtils.split(input, separator);
        Assert.assertArrayEquals(new String[]{"a", "", "b", "c"}, result.toArray());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSplit_NullInput() {
        StringUtils.split(null, ",");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSplit_EmptySeparator() {
        StringUtils.split("a,b,c", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSplit_NullSeparator() {
        StringUtils.split("a,b,c", null);
    }
}
