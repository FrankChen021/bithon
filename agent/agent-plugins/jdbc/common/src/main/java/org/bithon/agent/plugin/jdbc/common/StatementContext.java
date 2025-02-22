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

/**
 * @author frank.chen021@outlook.com
 * @date 2025/2/3 20:26
 */
public class StatementContext {
    public static final StatementContext EMPTY = new StatementContext("", "");

    private final String sql;
    private final String sqlType;

    public StatementContext(String sql) {
        this.sql = sql;
        this.sqlType = SqlTypeParser.parse(sql);
    }

    public StatementContext(String sql, String sqlType) {
        this.sql = sql;
        this.sqlType = sqlType;
    }

    public String getSql() {
        return sql;
    }

    public String getSqlType() {
        return sqlType;
    }
}
