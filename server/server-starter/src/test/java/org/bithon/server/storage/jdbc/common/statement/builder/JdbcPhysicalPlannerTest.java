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

package org.bithon.server.storage.jdbc.common.statement.builder;

import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.server.datasource.query.ast.Selector;
import org.bithon.server.datasource.query.plan.logical.ILogicalPlan;
import org.bithon.server.datasource.query.plan.logical.LogicalTableScan;
import org.bithon.server.datasource.query.plan.physical.IPhysicalPlan;
import org.bithon.server.datasource.reader.h2.H2SqlDialect;
import org.bithon.server.datasource.reader.jdbc.statement.JdbcPhysicalPlanner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/6/5 20:58
 */
public class JdbcPhysicalPlannerTest {
    @Test
    public void testTableScanPlan() {
        ILogicalPlan logicalPlan = new LogicalTableScan("test_table",
                                                        List.of(new Selector("a1", "a1", IDataType.DOUBLE)),
                                                        null);
        IPhysicalPlan physicalPlan = new JdbcPhysicalPlanner(null, new H2SqlDialect()).plan(logicalPlan);
        Assertions.assertEquals("""
                                TableScan
                                SELECT "a1" AS "a1"
                                FROM "test_table" AS "test_table"
                                """, physicalPlan.serializeToText());
    }

    @Test
    public void testTableScanPlanWithFilter() {
        ILogicalPlan logicalPlan = new LogicalTableScan("test_table",
                                                        List.of(new Selector("a1", "a1", IDataType.DOUBLE)),
                                                        new LogicalExpression.AND(new ComparisonExpression.EQ(IdentifierExpression.of("a1"), LiteralExpression.ofString("jacky"))));
        IPhysicalPlan physicalPlan = new JdbcPhysicalPlanner(null, new H2SqlDialect()).plan(logicalPlan);
        Assertions.assertEquals("""
                                TableScan
                                SELECT "a1" AS "a1"
                                FROM "test_table" AS "test_table"
                                WHERE ("a1" = 'jacky')
                                """, physicalPlan.serializeToText());
    }
}
