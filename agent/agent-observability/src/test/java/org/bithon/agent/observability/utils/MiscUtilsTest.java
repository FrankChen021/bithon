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

package org.bithon.agent.observability.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/21 22:28
 */
public class MiscUtilsTest {

    @Test
    public void testCleanupConnectionString_WithQueryParameters() {
        String input = "jdbc:mysql://localhost:3306/mydb?user=root&password=secret&useSSL=false";
        String expected = "jdbc:mysql://localhost:3306/mydb";
        String result = MiscUtils.cleanupConnectionString(input);
        assertEquals(expected, result);
    }

    @Test
    public void testCleanupConnectionString_WithSemicolonParameters() {
        String input = "jdbc:mysql://localhost:3306/mydb;user=root;password=secret;useSSL=false";
        String expected = "jdbc:mysql://localhost:3306/mydb";
        String result = MiscUtils.cleanupConnectionString(input);
        assertEquals(expected, result);
    }

    @Test
    public void testCleanupConnectionString_WithBothQueryAndSemicolonParameters() {
        String input = "jdbc:mysql://localhost:3306/mydb?user=root&password=secret;useSSL=false";
        String expected = "jdbc:mysql://localhost:3306/mydb";
        String result = MiscUtils.cleanupConnectionString(input);
        assertEquals(expected, result);
    }

    @Test
    public void testCleanupConnectionString_NoParameters() {
        String input = "jdbc:mysql://localhost:3306/mydb";
        String expected = "jdbc:mysql://localhost:3306/mydb";
        String result = MiscUtils.cleanupConnectionString(input);
        assertEquals(expected, result);
    }

    @Test
    public void testCleanupConnectionString_OnlyQueryParameter() {
        String input = "jdbc:postgresql://localhost:5432/testdb?user=admin";
        String expected = "jdbc:postgresql://localhost:5432/testdb";
        String result = MiscUtils.cleanupConnectionString(input);
        assertEquals(expected, result);
    }

    @Test
    public void testCleanupConnectionString_OnlySemicolonParameter() {
        String input = "jdbc:oracle:thin:@localhost:1521:xe;user=scott";
        String expected = "jdbc:oracle:thin:@localhost:1521:xe";
        String result = MiscUtils.cleanupConnectionString(input);
        assertEquals(expected, result);
    }

    @Test
    public void testCleanupConnectionString_EmptyString() {
        String input = "";
        String expected = "";
        String result = MiscUtils.cleanupConnectionString(input);
        assertEquals(expected, result);
    }

    @Test
    public void testCleanupConnectionString_NullString() {
        String input = null;
        String expected = null;
        String result = MiscUtils.cleanupConnectionString(input);
        assertEquals(expected, result);
    }

    @Test
    public void testCleanupConnectionString_OnlyQuestionMark() {
        String input = "?";
        String expected = "";
        String result = MiscUtils.cleanupConnectionString(input);
        assertEquals(expected, result);
    }

    @Test
    public void testCleanupConnectionString_OnlySemicolon() {
        String input = ";";
        String expected = "";
        String result = MiscUtils.cleanupConnectionString(input);
        assertEquals(expected, result);
    }

    @Test
    public void testCleanupConnectionString_ComplexUrl() {
        String input = "jdbc:mysql://user:pass@host1:3306,host2:3307/database?useSSL=true&serverTimezone=UTC;autoReconnect=true";
        String expected = "jdbc:mysql://user:pass@host1:3306,host2:3307/database";
        String result = MiscUtils.cleanupConnectionString(input);
        assertEquals(expected, result);
    }

    @Test
    public void testCleanupConnectionString_SQLServer() {
        String input = "jdbc:sqlserver://localhost:1433;databaseName=testdb;user=sa;password=password123";
        String expected = "jdbc:sqlserver://localhost:1433";
        String result = MiscUtils.cleanupConnectionString(input);
        assertEquals(expected, result);
    }

    @Test
    public void testCleanupConnectionString_H2() {
        String input = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        String expected = "jdbc:h2:mem:testdb";
        String result = MiscUtils.cleanupConnectionString(input);
        assertEquals(expected, result);
    }
}
