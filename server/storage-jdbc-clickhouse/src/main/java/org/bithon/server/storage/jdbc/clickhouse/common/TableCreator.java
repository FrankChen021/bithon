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

package org.bithon.server.storage.jdbc.clickhouse.common;

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.bithon.server.storage.jdbc.common.jooq.Tables;
import org.jooq.DSLContext;
import org.jooq.DataType;
import org.jooq.Field;
import org.jooq.Index;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.SQLDataType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        // Create local table
        //
        {
            //
            // NOTE: for ReplacingMergeTree, if the version is not specified on CREATE table DDL,
            // the last record will be kept
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
                                                           getFieldDeclarationExpression(table, true),
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
                    if (!params.isEmpty()) {
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
                for (UniqueKey<?> uk : table.getKeys()) {
                    if (uk.getFields().size() == 1 && this.secondaryIndexes.containsKey(uk.getFields().get(0).getName())) {
                        // index on single column,
                        // and is marked as secondary index,
                        // no need to put this column in the ORDER-BY expression as the primary key
                        continue;
                    }

                    for (TableField<?, ?> f : uk.getFields()) {
                        if ("timestamp".equals(f.getName())) {
                            createTableStatement.append(StringUtils.format("toStartOfMinute(timestamp),"));
                        } else {
                            createTableStatement.append(StringUtils.format("%s,", f.getName()));
                        }
                    }
                }
                for (Index idx : table.getIndexes()) {
                    if (idx.getFields().size() == 1 && this.secondaryIndexes.containsKey(idx.getFields().get(0).getName())) {
                        // index on single column,
                        // and is marked as secondary index,
                        // no need to put this column in the ORDER-BY expression as the primary key
                        continue;
                    }

                    if (replacingMergeTreeVersion != null) {
                        if (!idx.getUnique()) {
                            // For replacing the merge tree, use unique key as the order key only
                            continue;
                        }
                    }
                    for (SortField<?> f : idx.getFields()) {
                        if ("timestamp".equals(f.getName())) {
                            createTableStatement.append(StringUtils.format("toStartOfMinute(timestamp),"));
                        } else {
                            createTableStatement.append(StringUtils.format("%s,", f.getName()));
                        }
                    }
                }
                createTableStatement.delete(createTableStatement.length() - 1, createTableStatement.length());
                createTableStatement.append(")");
            }

            // ONLY partitioned table supports customized TTL
            if (partitionByExpression != null && !StringUtils.isEmpty(config.getTtl())) {
                boolean hasTimestamp = false;

                Matcher matcher = Pattern.compile("\\(([a-zA-Z_][a-zA-Z0-9_]*)\\)").matcher(config.getTtl());
                if (matcher.find()) {
                    String column = matcher.group(1);

                    for (Field<?> field : table.fields()) {
                        if (field.getName().equals(column)) {
                            String typeName = field.getDataType().getTypeName();
                            if ("timestamp".equals(typeName) || "datetime".equals(typeName)) {
                                hasTimestamp = true;
                                break;
                            } else {
                                throw new IllegalStateException("TTL expression is built upon a non-timestamp column [" + column + ", type=" + typeName + "]");
                            }
                        }
                    }
                }

                if (hasTimestamp) {
                    createTableStatement.append("\nTTL ");
                    createTableStatement.append(config.getTtl());
                }
            }

            if (!StringUtils.isEmpty(config.getCreateTableSettings())) {
                createTableStatement.append("\nSETTINGS ");
                createTableStatement.append(config.getCreateTableSettings());
            }

            String statement = createTableStatement.toString();
            log.info("CreateIfNotExists {}", tableName);
            log.debug("DDL\n {}", statement);
            dslContext.execute(statement);
        }

        //
        // Create distributed table if necessary.
        // In such a case, the table.getName() points to the distributed table.
        //
        if (config.isOnDistributedTable()) {
            StringBuilder sb = new StringBuilder(128);
            sb.append(StringUtils.format("CREATE TABLE IF NOT EXISTS `%s`.`%s` %s (%n",
                                         config.getDatabase(),
                                         table.getName(),
                                         config.getOnClusterExpression()));
            sb.append(getFieldDeclarationExpression(table, false));

            final StringBuilder shardingKey = new StringBuilder();
            if (this.replacingMergeTreeVersion != null) {
                // For the replacing merge tree,
                // we use the unique keys as the sharding key
                // to make sure the rows with the same key will be distributed to the same CK node
                Index uniqIndex = table.getIndexes()
                                       .stream()
                                       .filter(Index::getUnique)
                                       .findFirst()
                                       .orElse(null);

                if (uniqIndex != null) {
                    shardingKey.append("murmurHash2_64(");
                    shardingKey.append(uniqIndex.getFields()
                                                .stream()
                                                .map(SortField::getName)
                                                .collect(Collectors.joining(",")));
                    shardingKey.append(")");
                }
            }
            if (shardingKey.isEmpty()) {
                // Set a default sharding key for random writing
                shardingKey.append("rand()");
            }

            sb.append(StringUtils.format(") ENGINE=Distributed('%s', '%s', '%s', %s);",
                                         config.getCluster(),
                                         config.getDatabase(),
                                         table.getName() + "_local",
                                         shardingKey));

            log.info("CreateIfNotExists {}", table.getName());
            dslContext.execute(sb.toString());
        }
    }

    private String getFieldDeclarationExpression(Table<?> table, boolean isCodecSupported) {

        StringBuilder sb = new StringBuilder(128);
        for (Field<?> field : table.fields()) {
            DataType<?> dataType = field.getDataType();

            String typeName = dataType.getTypeName();
            if (dataType.equals(SQLDataType.TIMESTAMP) || dataType.equals(SQLDataType.LOCALDATETIME)) {
                typeName = "timestamp(3,0)";
            } else if (useMapType(table, field)) {
                typeName = "Map(String, String)";
            } else {
                if (dataType.hasPrecision()) {
                    typeName = dataType.getTypeName() + "(" + dataType.precision() + ", " + dataType.scale() + ")";
                }
            }

            sb.append(StringUtils.format("`%s` %s", field.getName(), typeName));

            Field<?> defaultValue = dataType.defaultValue();
            if (defaultValue != null) {
                String defaultValueText = defaultValue.toString();
                if (defaultValueText.toUpperCase(Locale.ENGLISH).startsWith("CURRENT_TIMESTAMP")) {
                    defaultValueText = "now()";
                }
                sb.append(StringUtils.format(" DEFAULT %s", defaultValueText));
            }

            if (isCodecSupported) {
                if (Number.class.isAssignableFrom(dataType.getType())) {
                    sb.append(" CODEC(T64, ZSTD)");
                } else if (String.class.equals(dataType.getType())) {
                    sb.append(" CODEC(ZSTD(1))");
                }
            }

            sb.append(",\n");
        }
        sb.delete(sb.length() - 2, sb.length());
        return sb.toString();
    }

    private boolean useMapType(Table<?> table, Field<?> field) {
        return Tables.BITHON_TRACE_SPAN.ATTRIBUTES.getName().equals(field.getName())
               && (table.getName().equals(Tables.BITHON_TRACE_SPAN.getName()) || table.getName().equals(Tables.BITHON_TRACE_SPAN_SUMMARY.getName()));
    }

    private String getIndexText() {
        StringBuilder sb = new StringBuilder(128);
        for (Map.Entry<String, SecondaryIndex> entry : this.secondaryIndexes.entrySet()) {
            String field = entry.getKey();
            SecondaryIndex idx = entry.getValue();

            String indexName = StringUtils.hasText(idx.getIndexName()) ? idx.getIndexName() : "idx_" + field;
            sb.append(StringUtils.format(",%nINDEX %s %s TYPE %s GRANULARITY %d", indexName, field, idx.getType(), idx.getGranularity()));
        }
        return sb.toString();
    }
}
