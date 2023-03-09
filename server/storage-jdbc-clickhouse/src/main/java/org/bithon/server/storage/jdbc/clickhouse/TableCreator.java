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
import org.bithon.component.commons.utils.StringUtils;
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Index;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;

/**
 * @author frank.chen021@outlook.com
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

    private boolean useReplacingMergeTree = false;

    /**
     * the version field of a ReplacingMergeTree
     */
    private String replacingMergeTreeVersion = "timestamp";

    private String partitionByExpression = "toYYYYMMDD(timestamp)";

    public TableCreator useReplacingMergeTree(boolean useReplacingMergeTree) {
        this.useReplacingMergeTree = useReplacingMergeTree;
        return this;
    }

    public TableCreator replacingMergeTreeVersion(String replacingMergeTreeVersion) {
        this.replacingMergeTreeVersion = replacingMergeTreeVersion;
        return this;
    }

    public TableCreator partitionByExpression(String partitionByExpression) {
        this.partitionByExpression = partitionByExpression;
        return this;
    }

    public void createIfNotExist(Table<?> table) {
        //
        // create local table
        //
        {
            //
            // NOTE: for ReplacingMergeTree, version is not specified on CREATE table DDL
            // that means the last record will be kept
            //
            String fullEngine = config.getEngine();
            if (useReplacingMergeTree && !config.getTableEngine().contains("Replacing")) {
                // turn the engine name into xxxReplacingMergeTree
                String enginePrefix = config.getTableEngine().substring(0, config.getTableEngine().length() - "MergeTree".length());
                String tableEngine = enginePrefix + "ReplacingMergeTree";

                int parenthesesIndex = fullEngine.indexOf('(');
                if (parenthesesIndex > 0) {
                    fullEngine = tableEngine + fullEngine.substring(parenthesesIndex);
                } else {
                    fullEngine = tableEngine;
                }
            }

            StringBuilder createTableStatement = new StringBuilder();

            String tableName = config.getLocalTableName(table.getName());
            createTableStatement.append(StringUtils.format("CREATE TABLE IF NOT EXISTS `%s`.`%s` %s (%n%s)",
                                                           config.getDatabase(),
                                                           tableName,
                                                           config.getOnClusterExpression(),
                                                           getFieldText(table)));

            // replace macro in the template to suit for ReplicatedMergeTree
            fullEngine = fullEngine.replaceAll("\\{database}", config.getDatabase())
                                   .replaceAll("\\{table}", tableName);

            if (useReplacingMergeTree) {
                // Insert the version field for ReplacingMergeTree
                int openParentheses = fullEngine.indexOf('(');
                int closeParentheses = fullEngine.lastIndexOf(')');
                if (openParentheses == -1 && closeParentheses == -1) {
                    fullEngine += "(" + replacingMergeTreeVersion + ")";
                } else {
                    String params = fullEngine.substring(openParentheses + 1, closeParentheses).trim();

                    // Remove the close first
                    fullEngine = fullEngine.substring(0, closeParentheses);
                    if (params.length() > 0) {
                        fullEngine += ", ";
                    }

                    fullEngine += replacingMergeTreeVersion;
                    fullEngine += ')';
                }

            }
            createTableStatement.append("\nENGINE=");
            createTableStatement.append(fullEngine);

            if (partitionByExpression != null) {
                createTableStatement.append("\nPARTITION BY ");
                createTableStatement.append(partitionByExpression);
                createTableStatement.append(' ');
            }

            //
            // Order by Clause
            //
            {
                createTableStatement.append("\nORDER BY(");
                for (Index idx : table.getIndexes()) {
                    if (useReplacingMergeTree) {
                        if (!idx.getUnique()) {
                            // for replacing merge tree, use unique key as order key only
                            continue;
                        }
                    }
                    for (SortField<?> f : idx.getFields()) {
                        if ("timestamp".equals(f.getName())) {
                            createTableStatement.append(StringUtils.format("toStartOfHour(timestamp),"));
                        } else {
                            createTableStatement.append(StringUtils.format("%s,", f.getName()));
                        }
                    }
                }
                createTableStatement.delete(createTableStatement.length() - 1, createTableStatement.length());
                createTableStatement.append(")");
            }

            if (!StringUtils.isEmpty(config.getCreateTableSettings())) {
                createTableStatement.append("\nSETTINGS ");
                createTableStatement.append(config.getCreateTableSettings());
            }
            createTableStatement.append(";");

            String statement = createTableStatement.toString();
            log.info("CreateIfNotExists {}", tableName);
            log.debug("DDL\n {}", statement);
            dslContext.execute(statement);
        }

        //
        // Create distributed table if necessary.
        // In such case, the table.getName() points to the distributed table.
        //
        if (config.isOnDistributedTable()) {
            StringBuilder sb = new StringBuilder(128);
            sb.append(StringUtils.format("CREATE TABLE IF NOT EXISTS `%s`.`%s` %s (%n",
                                         config.getDatabase(),
                                         table.getName(),
                                         config.getOnClusterExpression()));
            sb.append(getFieldText(table));
            sb.append(StringUtils.format(") ENGINE=Distributed('%s', '%s', '%s', murmurHash2_64(%s));",
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
            DataType dataType = f.getDataType();
            if (dataType.equals(SQLDataType.TIMESTAMP) ||dataType.equals(SQLDataType.LOCALDATETIME)) {
                sb.append(StringUtils.format("`%s` %s(3,0) ,%n",
                                             f.getName(),
                                             dataType.getTypeName()));
                continue;
            }
            if (dataType.hasPrecision()) {
                sb.append(StringUtils.format("`%s` %s(%d, %d) ,%n",
                                             f.getName(),
                                             dataType.getTypeName(),
                                             dataType.precision(),
                                             dataType.scale()));
            } else {
                sb.append(StringUtils.format("`%s` %s ,%n", f.getName(), dataType.getTypeName()));
            }
        }
        sb.delete(sb.length() - 2, sb.length());
        return sb.toString();
    }
}
