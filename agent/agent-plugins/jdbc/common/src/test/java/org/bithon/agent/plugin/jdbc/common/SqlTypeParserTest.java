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


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/2/3 20:16
 */
public class SqlTypeParserTest {

    @Test
    public void test_ParseSelect() {
        String sql = "select * FROM users";
        Assertions.assertEquals("SELECT", SqlTypeParser.parse(sql));
    }

    @Test
    public void test_ParseInsert() {
        String sql = "INSERT INTO users (id, name) VALUES (1, 'John')";
        Assertions.assertEquals("INSERT", SqlTypeParser.parse(sql));
    }

    @Test
    public void test_ParseUpdate() {
        String sql = "UPDATE users SET name = 'John' WHERE id = 1";
        Assertions.assertEquals("UPDATE", SqlTypeParser.parse(sql));
    }

    @Test
    public void test_ParseDelete() {
        String sql = "DELETE FROM users WHERE id = 1";
        Assertions.assertEquals("DELETE", SqlTypeParser.parse(sql));
    }

    @Test
    public void test_ParseWithSingleLineComment() {
        String sql = "-- This is a comment\nSELECT * FROM users";
        Assertions.assertEquals("SELECT", SqlTypeParser.parse(sql));
    }

    @Test
    public void test_ParseWithMultiLineComment_1() {
        String sql = "/* This is a\n" +
                     "multi-line comment */SELECT * FROM users";
        Assertions.assertEquals("SELECT", SqlTypeParser.parse(sql));
    }

    @Test
    public void test_ParseWithMultiLineComment_2() {
        String sql = "/* This is a\n" +
                     "multi-line comment */--SELECT * FROM users\nSELECT * FROM user";
        Assertions.assertEquals("SELECT", SqlTypeParser.parse(sql));
    }

    @Test
    public void test_ParseWithMultiLineComment_3() {
        String sql = "/* This is a\n" +
                     "multi-line comment */\n" +
                     "SELECT * FROM user";
        Assertions.assertEquals("SELECT", SqlTypeParser.parse(sql));
    }

    @Test
    public void test_ParseWithStringLiteral() {
        String sql = "SELECT * FROM users WHERE name = 'John'";
        Assertions.assertEquals("SELECT", SqlTypeParser.parse(sql));
    }

    @Test
    public void test_ParseLowerCase() {
        String sql = " select * FROM users WHERE name = 'John'";
        Assertions.assertEquals("SELECT", SqlTypeParser.parse(sql));
    }

    @Test
    public void test_MaxCharacter() {
        String sql = " ABCDEFGHIJK";
        Assertions.assertEquals("ABCDEFGHIJ", SqlTypeParser.parse(sql));
    }
}
