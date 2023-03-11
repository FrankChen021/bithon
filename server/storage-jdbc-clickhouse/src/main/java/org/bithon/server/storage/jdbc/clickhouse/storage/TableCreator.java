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

package org.bithon.server.storage.jdbc.clickhouse.storage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Index;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 1/11/21 6:48 pm
 */
@Slf4j
public class TableCreator {

    @Getter
    @AllArgsConstructor
    static class SecondaryIndex {
        private String type;
        private int granularity;
    }

    private final ClickHouseConfig config;
    private final DSLContext dslContext;

    public TableCreator(ClickHouseConfig config, DSLContext dslContext) {
        this.config = config;
        this.dslContext = dslContext;
    }

    /**
     * The version field of a ReplacingMergeTree
     */
    private String replacingMergeTreeVersion;

    private String partitionByExpression = "toYYYYMMDD(timestamp)";

    private final Map<String, SecondaryIndex> secondaryIndexes = new HashMap<>();

    public TableCreator useReplacingMergeTree(String versionField) {
        this.replacingMergeTreeVersion = versionField;
        return this;
    }

    public TableCreator partitionByExpression(String partitionByExpression) {
        this.partitionByExpression = partitionByExpression;
        return this;
    }

    public TableCreator secondaryIndex(String field, SecondaryIndex index) {
        secondaryIndexes.put(field, index);
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
            if (replacingMergeTreeVersion != null && !config.getTableEngine().contains("Replacing")) {
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
            createTableStatement.append(StringUtils.format("CREATE TABLE IF NOT EXISTS `%s`.`%s` %s (%n%s %s)",
                                                           config.getDatabase(),
                                                           tableName,
                                                           config.getOnClusterExpression(),
                                                           getFieldText(table),
                                                           getIndexText()));

            // replace macro in the template to suit for ReplicatedMergeTree
            fullEngine = fullEngine.replaceAll("\\{database}", config.getDatabase())
                                   .replaceAll("\\{table}", tableName);

            if (replacingMergeTreeVersion != null) {
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
                    if (idx.getFields().size() == 1 && this.secondaryIndexes.containsKey(idx.getFields().get(0).getName())) {
                        // index on single column, and is marked as secondary index, no need to put this column in the ORDER-BY expression as primary key
                        continue;
                    }

                    if (replacingMergeTreeVersion != null) {
                        if (!idx.getUnique()) {
                            // For replacing merge tree, use unique key as order key only,
                            // So if it's not the unique key, skip it
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
            DataType<?> dataType = f.getDataType();

            String typeName = dataType.getTypeName();
            if (dataType.equals(SQLDataType.TIMESTAMP) || dataType.equals(SQLDataType.LOCALDATETIME)) {
                typeName = "timestamp(3,0)";
            } else {
                if (dataType.hasPrecision()) {
                    typeName = dataType.getTypeName() + "(" + dataType.precision() + ", " + dataType.scale() + ")";
                }
            }

            sb.append(StringUtils.format("`%s` %s ", f.getName(), typeName));

            Field<?> defaultValue = dataType.defaultValue();
            if (defaultValue != null) {
                sb.append(StringUtils.format("DEFAULT %s", defaultValue.toString()));
            }

            sb.append(",\n");
        }
        sb.delete(sb.length() - 2, sb.length());
        return sb.toString();
    }

    private String getIndexText() {
        StringBuilder sb = new StringBuilder(128);
        for (Map.Entry<String, SecondaryIndex> entry : this.secondaryIndexes.entrySet()) {
            String field = entry.getKey();
            SecondaryIndex idx = entry.getValue();

            sb.append(StringUtils.format(",%nINDEX idx_%s %s TYPE %s GRANULARITY %d", field, field, idx.getType(), idx.getGranularity()));
        }
        return sb.toString();
    }
}
