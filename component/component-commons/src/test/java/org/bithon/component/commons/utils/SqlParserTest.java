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

/**
 * @author frank.chen021@outlook.com
 * @date 2025/1/17 23:44
 */
public class SqlParserTest {

    @Test
    public void test_parseSqlType_shouldReturnSelect_whenSqlIsSelect() {
        Assert.assertEquals("SELECT", SqlParser.parseSqlType("SELECT * FROM table"));
    }

    @Test
    public void test_parseSqlType_shouldReturnEmpty_whenSqlIsEmpty() {
        Assert.assertEquals("", SqlParser.parseSqlType(""));
    }

    @Test
    public void test_parseSqlType_shouldReturnEmpty_whenSqlIsCommentOnly() {
        Assert.assertEquals("", SqlParser.parseSqlType("-- This is a comment"));
    }

    @Test
    public void test_parseSqlType_shouldReturnInsert_whenSqlContainsSingleLineComment() {
        Assert.assertEquals("INSERT", SqlParser.parseSqlType("-- This is a comment\nINSERT INTO table VALUES (1)"));
    }

    @Test
    public void test_parseSqlType_shouldReturnUpdate_whenSqlContainsMultiLineComment() {
        Assert.assertEquals("UPDATE", SqlParser.parseSqlType("/* This is a comment */ UPDATE table SET column = 1"));
        Assert.assertEquals("UPDATE", SqlParser.parseSqlType("/* This is a \nmultiline comment */ UPDATE table SET column = 1"));
    }

    @Test
    public void test_parseSqlType_shouldReturnEmpty_whenSqlExceedsMaxSizeToLookFor() {
        Assert.assertEquals("SELECT", SqlParser.parseSqlType("SELECT * FROM table"));
    }

    @Test
    public void test_parseSqlType_shouldReturnDelete_whenSqlContainsWhitespace() {
        Assert.assertEquals("DELETE", SqlParser.parseSqlType("   DELETE FROM table"));
    }

    @Test
    public void test_parseSqlType_shouldReturnAlter_whenSqlContainsMixedComments() {
        Assert.assertEquals("ALTER", SqlParser.parseSqlType("/* comment */ -- comment\n ALTER TABLE table"));
    }
}
