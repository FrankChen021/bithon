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

package org.bithon.server.commons.utils;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/3/29 18:38
 */
public class DbUtilsTest {

    @Test
    public void testMySQL() {
        DbUtils.ConnectionString conn = DbUtils.parseConnectionString("jdbc:mysql://localhost:3306/test");
        Assertions.assertEquals("localhost:3306", conn.getHostAndPort());
        Assertions.assertEquals("test", conn.getDatabase());
        Assertions.assertEquals("mysql", conn.getDbType());
    }

    @Test
    public void testMySQLWithoutDatabase() {
        DbUtils.ConnectionString conn = DbUtils.parseConnectionString("jdbc:mysql://localhost:3306");
        Assertions.assertEquals("localhost:3306", conn.getHostAndPort());
        Assertions.assertEquals("", conn.getDatabase());
        Assertions.assertEquals("mysql", conn.getDbType());
    }

    @Test
    public void testMySQLWithoutDatabase2() {
        DbUtils.ConnectionString conn = DbUtils.parseConnectionString("jdbc:mysql://localhost:3306/");
        Assertions.assertEquals("localhost:3306", conn.getHostAndPort());
        Assertions.assertEquals("", conn.getDatabase());
        Assertions.assertEquals("mysql", conn.getDbType());
    }

    @Test
    public void testMySQLReplica() {
        DbUtils.ConnectionString conn = DbUtils.parseConnectionString("jdbc:mysql:replication://db-master1.mobile.com:6606,db-slave.mobile.com:6606/db");
        Assertions.assertEquals("db-master1.mobile.com:6606", conn.getHostAndPort());
        Assertions.assertEquals("db", conn.getDatabase());
        Assertions.assertEquals("mysql", conn.getDbType());
    }

    @Test
    public void test_ClickHouseConnectionString_NoDatabase() {
        DbUtils.ConnectionString conn = DbUtils.parseConnectionString("jdbc:clickhouse://localhost:8123");
        Assertions.assertEquals("localhost:8123", conn.getHostAndPort());
        Assertions.assertEquals("", conn.getDatabase());
        Assertions.assertEquals("clickhouse", conn.getDbType());
    }

    @Test
    public void test_ClickHouseConnectionString_WithHTTP() {
        DbUtils.ConnectionString conn = DbUtils.parseConnectionString("jdbc:clickhouse:http://localhost:8123");
        Assertions.assertEquals("localhost:8123", conn.getHostAndPort());
        Assertions.assertEquals("", conn.getDatabase());
        Assertions.assertEquals("clickhouse", conn.getDbType());
    }

    @Test
    public void test_ClickHouseConnectionString_HTTPS() {
        DbUtils.ConnectionString conn = DbUtils.parseConnectionString("jdbc:clickhouse:https://localhost:403");
        Assertions.assertEquals("localhost:403", conn.getHostAndPort());
        Assertions.assertEquals("", conn.getDatabase());
        Assertions.assertEquals("clickhouse", conn.getDbType());
    }

    @Test
    public void test_ClickHouseConnectionString_WithDatabase() {
        DbUtils.ConnectionString conn = DbUtils.parseConnectionString("jdbc:clickhouse:https://localhost:403/bithon");
        Assertions.assertEquals("localhost:403", conn.getHostAndPort());
        Assertions.assertEquals("bithon", conn.getDatabase());
        Assertions.assertEquals("clickhouse", conn.getDbType());
    }
}
