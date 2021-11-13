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

package org.bithon.server.storage.jdbc.clickhouse;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class TableCreator {

    private final ClickHouseConfig config;
    private final DSLContext dslContext;

    public TableCreator(ClickHouseConfig config, DSLContext dslContext) {
        this.config = config;
        this.dslContext = dslContext;
    }

    public void createIfNotExist(Table<?> table, int ttlDays) {
        createIfNotExist(table, ttlDays, false);
    }

    public void createIfNotExist(Table<?> table, int ttlDays, boolean useReplacingMergeTree) {
        //
        // create local table
        //
        {
            //
            // NOTE: for ReplacingMergeTree, version is not specified on CREATE table DDL
            // that means the last record will be kept
            //
            String engine = config.getEngine();
            if (useReplacingMergeTree && !config.getTableEngine().contains("Replacing")) {
                // turn the engine into xxxReplacingMergeTree
                String enginePrefix = config.getTableEngine().substring(0, config.getTableEngine().length() - "MergeTree".length());
                String tableEngine = enginePrefix + "ReplacingMergeTree";

                int spaceIndex = engine.indexOf(' ');
                if (spaceIndex > 0) {
                    engine = tableEngine + engine.substring(spaceIndex + 1);
                } else {
                    engine = tableEngine;
                }
            }

            StringBuilder sb = new StringBuilder();

            String tableName = StringUtils.hasText(config.getCluster()) ? table.getName() + "_local" : table.getName();
            sb.append(String.format("CREATE TABLE IF NOT EXISTS `%s`.`%s` %s (%n",
                                    config.getDatabase(),
                                    tableName,
                                    StringUtils.hasText(config.getCluster()) ? " on cluster " + config.getCluster() : ""));
            sb.append(getFieldText(table));
            sb.append(String.format(") ENGINE=%s PARTITION BY toYYYYMMDD(timestamp) ", engine.replaceAll("\\{database\\}", config.getDatabase())
                                                                                             .replaceAll("\\{table\\}", tableName)));

            //
            // Order by Clause
            //
            {
                sb.append("ORDER BY(");
                for (Index idx : table.getIndexes()) {
                    for (SortField<?> f : idx.getFields()) {
                        sb.append(String.format("`%s`,", f.getName()));
                    }
                }
                sb.delete(sb.length() - 1, sb.length());
                sb.append(") ");
            }

            //
            // TTL Clause
            //
            if (ttlDays > 0) {
                //
                // NOTE: the timestamp type we're using is DateTime64 which is not supported as TTL expression by now
                // So, we have to use ALTER command to delete partitions by ourselves
                //
                //sb.append(String.format("TTL timestamp + INTERVAL %d DAY", ttlDays));
            }
            sb.append(";");

            log.info("CreateIfNotExists {}", tableName);
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
            sb.append(String.format("CREATE TABLE IF NOT EXISTS `%s`.`%s` %s (%n",
                                    config.getDatabase(),
                                    table.getName(),
                                    StringUtils.hasText(config.getCluster()) ? " on cluster " + config.getCluster() : ""));
            sb.append(getFieldText(table));
            sb.append(String.format(") ENGINE=Distributed('%s', '%s', '%s', murmurHash2_64(%s));",
                                    config.getCluster(),
                                    config.getDatabase(),
                                    table.getName() + "_local",
                                    "bithon_topo_metrics".equals(table.getName()) ? "srcEndpoint" : "appName"));

            log.info("CreateIfNotExists {}", table.getName());
            dslContext.execute(sb.toString());
        }
    }

    private String getFieldText(Table<?> table) {
        StringBuilder sb = new StringBuilder(128);
        for (Field<?> f : table.fields()) {
            if (f.getDataType().equals(SQLDataType.TIMESTAMP)) {
                sb.append(String.format("`%s` %s(3,0) ,%n",
                                        f.getName(),
                                        f.getDataType().getTypeName()));
                continue;
            }
            if (f.getDataType().hasPrecision()) {
                sb.append(String.format("`%s` %s(%d, %d) ,%n",
                                        f.getName(),
                                        f.getDataType().getTypeName(),
                                        f.getDataType().precision(),
                                        f.getDataType().scale()));
            } else {
                sb.append(String.format("`%s` %s ,%n", f.getName(), f.getDataType().getTypeName()));
            }
        }
        sb.delete(sb.length() - 2, sb.length());
        return sb.toString();
    }
}
