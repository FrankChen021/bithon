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

package org.bithon.server.datasource.reader.clickhouse;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.datasource.expression.ExpressionASTBuilder;
import org.bithon.server.datasource.expression.ExpressionParser;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages ClickHouse table metadata including ORDER BY expressions.
 * Uses Guava cache to improve query performance.
 *
 * @author Frank Chen
 * @date 29/1/24 11:26 am
 */
@Slf4j
public class ClickHouseMetadataManager {

    private static final Duration CACHE_EXPIRATION_DURATION = Duration.ofMinutes(30);
    // Match ORDER BY followed by anything (handles both "ORDER BY x, y" and "ORDER BY (x, y)")
    // Captures everything until PARTITION/SETTINGS keyword or end of string
    private static final Pattern ORDER_BY_PATTERN = Pattern.compile("ORDER\\s+BY\\s+(.+?)(?:\\s+(?:PARTITION|SETTINGS)|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DISTRIBUTED_TABLE_PATTERN = Pattern.compile("Distributed\\([^,]+,\\s*'?([^',]+)'?,\\s*'?([^',]+)'?", Pattern.CASE_INSENSITIVE);

    private final DSLContext dslContext;
    private final LoadingCache<TableKey, List<IExpression>> orderByCache;

    public ClickHouseMetadataManager(DSLContext dslContext) {
        this.dslContext = dslContext;
        this.orderByCache = CacheBuilder.newBuilder()
                                        .expireAfterAccess(CACHE_EXPIRATION_DURATION)
                                        .build(new CacheLoader<>() {
                                            @Override
                                            public List<IExpression> load(TableKey key) {
                                                return fetchOrderByExpression(key.database, key.tableName);
                                            }
                                        });
    }

    /**
     * Get the ORDER BY expressions for a table.
     * Accepts table name in format "database.table" or just "table".
     * If the database is not provided, the query will not filter by database.
     *
     * @param tableReference table reference, either "database.table" or "table"
     * @return list of ORDER BY expressions
     * @throws RuntimeException if the table is not found or ORDER BY cannot be determined
     */
    public List<IExpression> getOrderByExpression(String tableReference) {
        String database = null;
        String tableName;

        int dotIndex = tableReference.indexOf('.');
        if (dotIndex > 0) {
            // Format: "database.table"
            database = tableReference.substring(0, dotIndex);
            tableName = tableReference.substring(dotIndex + 1);
        } else {
            // Format: "table" (no database specified)
            tableName = tableReference;
        }

        return getOrderByExpression(database, tableName);
    }

    /**
     * Get the ORDER BY expressions for a table.
     *
     * @param database  the database name (can be null if not filtering by database)
     * @param tableName the table name
     * @return list of ORDER BY expressions
     * @throws RuntimeException if the table is not found or ORDER BY cannot be determined
     */
    public List<IExpression> getOrderByExpression(String database, String tableName) {
        try {
            return orderByCache.get(new TableKey(database, tableName));
        } catch (ExecutionException e) {
            String fullTableName = database != null ? database + "." + tableName : tableName;
            throw new RuntimeException(StringUtils.format("Failed to get ORDER BY expression for table %s", fullTableName), e.getCause());
        }
    }

    /**
     * Fetches and parses the ORDER BY expression from ClickHouse system.tables.
     *
     * @param database  the database name (can be null to not filter by database)
     * @param tableName the table name
     */
    private List<IExpression> fetchOrderByExpression(String database, String tableName) {
        String fullTableName = database != null ? database + "." + tableName : tableName;
        log.info("Fetching ORDER BY expression for table {}", fullTableName);

        String sql;
        if (database != null) {
            sql = StringUtils.format(
                "SELECT engine_full FROM system.tables WHERE database = '%s' AND name = '%s'",
                database,
                tableName
            );
        } else {
            sql = StringUtils.format(
                "SELECT engine_full FROM system.tables WHERE name = '%s'",
                tableName
            );
        }

        List<Record> records = dslContext.fetch(sql);
        if (records.isEmpty()) {
            throw new RuntimeException(StringUtils.format("Table %s not found", fullTableName));
        }

        String engineFull = records.get(0).get(0, String.class);
        if (StringUtils.isEmpty(engineFull)) {
            throw new RuntimeException(StringUtils.format("engine_full is empty for table %s", fullTableName));
        }

        log.debug("engine_full for {}: {}", fullTableName, engineFull);

        // Check if this is a Distributed table
        Matcher distributedMatcher = DISTRIBUTED_TABLE_PATTERN.matcher(engineFull);
        if (distributedMatcher.find()) {
            String localDatabase = distributedMatcher.group(1);
            String localTable = distributedMatcher.group(2);
            log.info("Table {} is a Distributed table, fetching ORDER BY from local table {}.{}",
                     fullTableName, localDatabase, localTable);
            return fetchOrderByExpression(localDatabase, localTable);
        }

        // Extract ORDER BY clause
        Matcher orderByMatcher = ORDER_BY_PATTERN.matcher(engineFull);
        if (!orderByMatcher.find()) {
            throw new RuntimeException(StringUtils.format("No ORDER BY clause found in engine_full for table %s", fullTableName));
        }

        String orderByClause = orderByMatcher.group(1)
                                             .replaceAll("(?i)\\s+(DESC|ASC)\\s*", "")
                                             .trim();

        // Remove outer parentheses if present
        if (orderByClause.startsWith("(") && orderByClause.endsWith(")")) {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build(orderByClause, ExpressionParser::expressionListDecl);
            return expr instanceof ExpressionList ? ((ExpressionList) expr).getExpressions() : Collections.singletonList(expr);
        } else {
            return Collections.singletonList(ExpressionASTBuilder.builder()
                                                                 .build(orderByClause, ExpressionParser::expression));
        }
    }

    /**
     * Parses the ORDER BY clause string into a list of IExpression objects.
     */
    private List<IExpression> parseOrderByClause(String orderByClause) {
        List<IExpression> expressions = new ArrayList<>();

        log.info("parseOrderByClause input: [{}]", orderByClause);

        // Split by comma, but respect parentheses (don't split on commas inside function calls)
        List<String> fields = splitByComma(orderByClause);

        log.info("After splitByComma: {} fields", fields.size());
        for (int i = 0; i < fields.size(); i++) {
            log.info("  Field [{}]: [{}]", i, fields.get(i));
        }

        for (String field : fields) {
            String originalField = field;
            field = field.trim();
            log.info("After trim: [{}] -> [{}]", originalField, field);

            if (field.isEmpty()) {
                continue;
            }

            // Remove DESC/ASC suffix if present
            String beforeDescAsc = field;
            field = field.replaceAll("(?i)\\s+(DESC|ASC)\\s*$", "").trim();
            log.info("After remove DESC/ASC: [{}] -> [{}]", beforeDescAsc, field);

            // Normalize spaces (remove extra spaces inside the expression)
            String beforeNormalize = field;
            field = field.replaceAll("\\s+", " ")
                         .replaceAll("\\(\\s+", "(")
                         .replaceAll("\\s+\\)", ")");
            log.info("After normalize: [{}] -> [{}]", beforeNormalize, field);

            try {
                IExpression expression = ExpressionASTBuilder.builder()
                                                             .optimizationEnabled(false)
                                                             .build(field);
                expressions.add(expression);
                log.debug("Parsed ORDER BY field: {} -> {}", field, expression.serializeToText());
            } catch (Exception e) {
                throw new RuntimeException(StringUtils.format("Failed to parse ORDER BY field: %s", field), e);
            }
        }

        if (expressions.isEmpty()) {
            throw new RuntimeException("No valid ORDER BY expressions found");
        }

        return expressions;
    }

    /**
     * Split a string by comma, but respect parentheses.
     * For example: "toStartOfMinute(timestamp, 'UTC'), appName" will be split into:
     * ["toStartOfMinute(timestamp, 'UTC')", "appName"]
     */
    private List<String> splitByComma(String input) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int parenthesesLevel = 0;

        for (char c : input.toCharArray()) {
            if (c == '(') {
                parenthesesLevel++;
                current.append(c);
            } else if (c == ')') {
                parenthesesLevel--;
                current.append(c);
            } else if (c == ',' && parenthesesLevel == 0) {
                // Only split on commas outside of parentheses
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }

        // Add the last field
        if (!current.isEmpty()) {
            result.add(current.toString());
        }

        return result;
    }

    /**
     * Cache key for ORDER BY expressions.
     */
    private record TableKey(String database, String tableName) {

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TableKey tableKey = (TableKey) o;
            return database.equals(tableKey.database) && tableName.equals(tableKey.tableName);
        }

    }
}

