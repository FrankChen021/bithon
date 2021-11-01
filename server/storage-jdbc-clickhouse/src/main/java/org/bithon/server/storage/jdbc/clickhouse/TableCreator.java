/*
 *    Copyright 2020 bithon.cn
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

package org.bithon.server.storage.jdbc.clickhouse;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Index;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;
import org.springframework.util.StringUtils;

/**
 * @author Frank Chen
 * @date 1/11/21 6:48 pm
 */
public class TableCreator {

    private final ClickHouseConfig config;
    private final DSLContext dslContext;

    public TableCreator(ClickHouseConfig config, DSLContext dslContext) {
        this.config = config;
        this.dslContext = dslContext;
    }

    public void createIfNotExist(Table<?> table) {
        //
        // create local table
        //
        {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("CREATE TABLE IF NOT EXISTS `%s`.`%s` %s (\n",
                                    config.getDatabase(),
                                    StringUtils.hasText(config.getCluster()) ? table.getName() + "_local" : table.getName(),
                                    StringUtils.hasText(config.getCluster()) ? " on cluster " + config.getCluster() : ""));
            sb.append(getFieldText(table));
            sb.append(String.format(") ENGINE=%s PARTITION BY toYYYYMMDD(timestamp) ORDER BY(", config.getEngine()));
            for (Index idx : table.getIndexes()) {
                for (SortField<?> f : idx.getFields()) {
                    sb.append(String.format("`%s`,", f.getName()));
                }
            }
            sb.delete(sb.length() - 1, sb.length());
            sb.append(");");
            dslContext.execute(sb.toString());
        }

        if (!StringUtils.hasText(config.getCluster())) {
            return;
        }
        {
            //
            // create distributed table
            //
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("CREATE TABLE IF NOT EXISTS `%s`.`%s` %s (\n",
                                    config.getDatabase(),
                                    table.getName(),
                                    StringUtils.hasText(config.getCluster()) ? " on cluster " + config.getCluster() : ""));
            sb.append(getFieldText(table));
            sb.append(String.format(") ENGINE=Distributed('%s', '%s', '%s', murmurHash2_64(%s));",
                                    config.getCluster(),
                                    config.getDatabase(),
                                    table.getName() + "_local",
                                    "bithon_topo_metrics".equals(table.getName()) ? "srcEndpoint" : "appName"));
            dslContext.execute(sb.toString());
        }
    }

    private String getFieldText(Table<?> table) {
        StringBuilder sb = new StringBuilder(128);
        for (Field<?> f : table.fields()) {
            if (f.getDataType().equals(SQLDataType.TIMESTAMP)) {
                sb.append(String.format("`%s` %s(3, 0) ,\n",
                                        f.getName(),
                                        f.getDataType().getTypeName()));
                continue;
            }
            if (f.getDataType().hasPrecision()) {
                sb.append(String.format("`%s` %s(%d, %d) ,\n",
                                        f.getName(),
                                        f.getDataType().getTypeName(),
                                        f.getDataType().precision(),
                                        f.getDataType().scale()));
            } else {
                sb.append(String.format("`%s` %s ,\n", f.getName(), f.getDataType().getTypeName()));
            }
        }
        sb.delete(sb.length() - 2, sb.length());
        return sb.toString();
    }
}
