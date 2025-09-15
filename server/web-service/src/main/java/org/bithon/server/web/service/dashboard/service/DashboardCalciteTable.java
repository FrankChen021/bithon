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

package org.bithon.server.web.service.dashboard.service;

import org.apache.calcite.DataContext;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Linq4j;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.schema.ScannableTable;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.bithon.server.storage.dashboard.Dashboard;

import java.util.Iterator;
import java.util.Map;

/**
 * Calcite Table for in-memory dashboard data
 *
 * @author Frank Chen
 * @date 2025-09-11
 */
public class DashboardCalciteTable extends AbstractTable implements ScannableTable {

    private final Map<String, Dashboard> dashboards;

    public DashboardCalciteTable(Map<String, Dashboard> dashboards) {
        this.dashboards = dashboards;
    }

    @Override
    public RelDataType getRowType(RelDataTypeFactory typeFactory) {
        RelDataTypeFactory.Builder builder = typeFactory.builder();
        builder.add("id", typeFactory.createSqlType(SqlTypeName.VARCHAR));
        builder.add("title", typeFactory.createSqlType(SqlTypeName.VARCHAR));
        builder.add("folder", typeFactory.createSqlType(SqlTypeName.VARCHAR));
        builder.add("signature", typeFactory.createSqlType(SqlTypeName.VARCHAR));
        builder.add("createdAt", typeFactory.createSqlType(SqlTypeName.TIMESTAMP));
        builder.add("lastModified", typeFactory.createSqlType(SqlTypeName.TIMESTAMP));
        builder.add("visible", typeFactory.createSqlType(SqlTypeName.BOOLEAN));
        return builder.build();
    }

    @Override
    public Enumerable<Object[]> scan(DataContext root) {
        return Linq4j.asEnumerable(() -> new Iterator<>() {
            private final Iterator<Dashboard> dashboardIterator = dashboards.values().iterator();

            @Override
            public boolean hasNext() {
                return dashboardIterator.hasNext();
            }

            @Override
            public Object[] next() {
                Dashboard dashboard = dashboardIterator.next();
                return new Object[]{
                    dashboard.getId(),
                    dashboard.getTitle(),
                    dashboard.getFolder(),
                    dashboard.getSignature(),
                    dashboard.getCreatedAt(),
                    dashboard.getLastModified(),
                    dashboard.isVisible()
                };
            }
        });
    }
}
