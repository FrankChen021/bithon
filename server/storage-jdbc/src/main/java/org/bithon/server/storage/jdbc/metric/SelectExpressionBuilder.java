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

package org.bithon.server.storage.jdbc.metric;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.MacroExpression;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.column.aggregatable.IAggregatableColumn;
import org.bithon.server.storage.datasource.query.ast.Column;
import org.bithon.server.storage.datasource.query.ast.ColumnAlias;
import org.bithon.server.storage.datasource.query.ast.Expression;
import org.bithon.server.storage.datasource.query.ast.GroupBy;
import org.bithon.server.storage.datasource.query.ast.IASTNode;
import org.bithon.server.storage.datasource.query.ast.Limit;
import org.bithon.server.storage.datasource.query.ast.Name;
import org.bithon.server.storage.datasource.query.ast.OrderBy;
import org.bithon.server.storage.datasource.query.ast.ResultColumn;
import org.bithon.server.storage.datasource.query.ast.SelectExpression;
import org.bithon.server.storage.datasource.query.ast.SimpleAggregateExpression;
import org.bithon.server.storage.datasource.query.ast.StringNode;
import org.bithon.server.storage.datasource.query.ast.Table;
import org.bithon.server.storage.datasource.query.ast.Where;
import org.bithon.server.storage.datasource.query.parser.FieldExpressionVisitorAdaptor2;
import org.bithon.server.storage.jdbc.common.dialect.Expression2Sql;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;
import org.bithon.server.storage.metrics.Interval;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/10/30 11:52
 */
public class SelectExpressionBuilder {

    private ISchema schema;

    private List<ResultColumn> resultColumns;

    private IExpression filter;
    private Interval interval;

    private List<String> groupBy;

    @Nullable
    private org.bithon.server.storage.datasource.query.OrderBy orderBy;

    @Nullable
    private org.bithon.server.storage.datasource.query.Limit limit;

    private ISqlDialect sqlDialect;

    public static SelectExpressionBuilder builder() {
        return new SelectExpressionBuilder();
    }

    public SelectExpressionBuilder dataSource(ISchema dataSource) {
        this.schema = dataSource;
        return this;
    }

    public SelectExpressionBuilder fields(List<ResultColumn> resultColumns) {
        this.resultColumns = resultColumns;
        return this;
    }

    public SelectExpressionBuilder filter(IExpression filter) {
        this.filter = filter;
        return this;
    }

    public SelectExpressionBuilder interval(Interval interval) {
        this.interval = interval;
        return this;
    }

    public SelectExpressionBuilder groupBys(List<String> groupBy) {
        this.groupBy = groupBy;
        return this;
    }

    public SelectExpressionBuilder orderBy(@Nullable org.bithon.server.storage.datasource.query.OrderBy orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public SelectExpressionBuilder limit(@Nullable org.bithon.server.storage.datasource.query.Limit limit) {
        this.limit = limit;
        return this;
    }

    public SelectExpressionBuilder sqlDialect(ISqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
        return this;
    }

    static class FieldExpressionAnalyzer extends FieldExpressionVisitorAdaptor2 {

        private final Set<String> aggregatedColumn;
        private final ISqlDialect sqlExpressionFormatter;

        @Getter
        private final List<IAggregatableColumn> windowFunctionAggregators = new ArrayList<>();

        @Getter
        private final Set<String> metrics = new HashSet<>();
        private final ISchema schema;

        FieldExpressionAnalyzer(ISchema schema,
                                Set<String> aggregatedColumns,
                                ISqlDialect sqlFormatter) {
            this.schema = schema;
            this.aggregatedColumn = aggregatedColumns;
            this.sqlExpressionFormatter = sqlFormatter;
        }

        @Override
        protected ISchema getSchema() {
            return schema;
        }

        @Override
        public void visitField(IColumn columnSpec) {
            if (!(columnSpec instanceof IAggregatableColumn)) {
                throw new RuntimeException(StringUtils.format("field [%s] is not a metric", columnSpec.getName()));
            }

            IAggregatableColumn metricSpec = (IAggregatableColumn) columnSpec;
            if (aggregatedColumn.contains(metricSpec.getName())) {
                return;
            }

            if (sqlExpressionFormatter.useWindowFunctionAsAggregator(metricSpec.getAggregateExpression().getFnName())) {
                // The aggregator uses WindowFunction, it will be in a sub-query of generated SQL
                // So, we turn the metric into a pre-aggregator
                aggregatedColumn.add(metricSpec.getName());

                windowFunctionAggregators.add(metricSpec);
            } else {
                // this metric should also be in the sub-query expression
                metrics.add(metricSpec.getName());
            }
        }
    }

    static class SQLGenerator4Expression {
        private final ISqlDialect sqlDialect;
        private final SqlGenerator4SimpleAggregationFunction sqlGenerator4SimpleAggregationFunction;

        protected final Map<String, Object> internalVariables;
        private final Set<String> aggregatedFields;

        private final ISchema schema;

        SQLGenerator4Expression(ISchema schema,
                                ISqlDialect sqlDialect,
                                Set<String> aggregatedFields,
                                SqlGenerator4SimpleAggregationFunction sqlGenerator4SimpleAggregationFunction,
                                Map<String, Object> internalVariables) {
            this.schema = schema;
            this.sqlDialect = sqlDialect;
            this.aggregatedFields = aggregatedFields;
            this.sqlGenerator4SimpleAggregationFunction = sqlGenerator4SimpleAggregationFunction;
            this.internalVariables = internalVariables;
        }

        public StringNode visit(Expression expression) {

            Expression2Sql serializer = new Expression2Sql(null, this.sqlDialect) {
                @Override
                public boolean visit(IdentifierExpression expression) {
                    String field = expression.getIdentifier();
                    IColumn columnSpec = schema.getColumnByName(field);
                    if (columnSpec == null) {
                        throw new RuntimeException(StringUtils.format("field [%s] can't be found in [%s].", field, schema.getName()));
                    }

                    IAggregatableColumn metricSpec = (IAggregatableColumn) columnSpec;

                    // Case 1. The field used in window function is presented in a sub-query, at the root query level we only reference the name
                    boolean useWindowFunctionAsAggregator = sqlDialect.useWindowFunctionAsAggregator(metricSpec.getAggregateExpression().getFnName());

                    // Case 2. Some DB does not allow same aggregation expressions, we use the existing expression
                    boolean hasSameExpression = !sqlDialect.allowSameAggregatorExpression() && aggregatedFields.contains(metricSpec.getName());

                    if (useWindowFunctionAsAggregator || hasSameExpression) {
                        sb.append(sqlDialect.quoteIdentifier(metricSpec.getName()));
                    } else {
                        // generate a aggregation expression
                        sb.append(metricSpec.getAggregateExpression().accept(sqlGenerator4SimpleAggregationFunction));
                    }

                    return true;
                }

                @Override
                public boolean visit(MacroExpression expression) {
                    Object variableValue = internalVariables.get(expression.getMacro());
                    if (variableValue == null) {
                        throw new RuntimeException(StringUtils.format("variable (%s) not provided in context",
                                                                      expression.getMacro()));
                    }
                    sb.append(variableValue);

                    return true;
                }
            };

            return new StringNode(serializer.serialize(expression.getParsedExpression()));
        }
    }

    /**
     * Example result SQL if window function is used for first/last aggregator
     * <pre>
     * SELECT
     *   "timestamp" AS "_timestamp",
     *   sum("totalTaskCount") AS "totalTaskCount",
     *   queuedTaskCount
     * FROM
     *   (
     *     SELECT
     *       UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
     *       FIRST_VALUE("queuedTaskCount") OVER (
     *         partition by CAST(toUnixTimestamp("timestamp") / 10 AS Int64) * 10 ORDER BY "timestamp" DESC
     *       ) AS "queuedTaskCount",
     *       "totalTaskCount",
     *     FROM
     *       "bithon_thread_pool_metrics"
     *     WHERE
     *       "appName" = 'bithon-server-live'
     *       AND "timestamp" >= fromUnixTimestamp(1666578760)
     *       AND "timestamp" < fromUnixTimestamp(1666589560)
     *   )
     * GROUP BY
     *   "_timestamp", queuedTaskCount
     * ORDER BY
     *   "_timestamp"
     * </pre>
     */
    public SelectExpression build() {
        String sqlTableName = schema.getDataStoreSpec().getStore();

        //
        // Turn some metrics (those use window functions for aggregation) in expression into pre-aggregator first
        //
        Set<String> aggregatedFields = this.resultColumns.stream()
                                                         .filter((f) -> f.getColumnExpression() instanceof SimpleAggregateExpression)
                                                         .map(resultColumn -> ((SimpleAggregateExpression) resultColumn.getColumnExpression()).getTargetColumn())
                                                         .collect(Collectors.toSet());

        SelectExpression selectExpression = new SelectExpression();
        selectExpression.setGroupBy(new GroupBy());
        SelectExpression subSelectExpression = new SelectExpression();

        //
        // fields
        //
        boolean hasSubSelect = false;
        SqlGenerator4SimpleAggregationFunction generator = new SqlGenerator4SimpleAggregationFunction(sqlDialect,
                                                                                                      interval);

        SQLGenerator4Expression sqlGenerator4Expression = new SQLGenerator4Expression(schema,
                                                                                      sqlDialect,
                                                                                      aggregatedFields,
                                                                                      generator,
                                                                                      ImmutableMap.of("interval",
                                                                                                      interval.getStep() == null ? interval.getTotalLength() : interval.getStep(),
                                                                                                      "instanceCount",
                                                                                                      StringUtils.format("count(distinct %s)", sqlDialect.quoteIdentifier("instanceName"))));

        for (ResultColumn resultColumn : this.resultColumns) {
            IASTNode columnExpression = resultColumn.getColumnExpression();
            if (columnExpression instanceof SimpleAggregateExpression) {
                SimpleAggregateExpression function = (SimpleAggregateExpression) columnExpression;

                // if window function is contained, the final SQL has a sub-query
                if (sqlDialect.useWindowFunctionAsAggregator(function.getFnName())) {
                    subSelectExpression.getResultColumnList().add(new StringNode(function.accept(generator)), resultColumn.getAlias());

                    // this window fields should be in the group-by clause and select clause,
                    // see the javadoc above
                    // Use name in the groupBy expression because we have alias for the corresponding field in sub-query expression
                    selectExpression.getGroupBy().addField(resultColumn.getAlias().getName());
                    selectExpression.getResultColumnList().add(resultColumn.getAlias().getName());

                    hasSubSelect = true;
                } else {
                    selectExpression.getResultColumnList().add(new StringNode(function.accept(generator)), resultColumn.getAlias());

                    String underlyingFieldName = ((Name) function.getArguments().get(0)).getName();

                    // This metric should also be in the sub-query, see the example in the javadoc above
                    subSelectExpression.getResultColumnList().add(underlyingFieldName);
                }
            } else if (columnExpression instanceof Expression) {
                selectExpression.getResultColumnList().add(sqlGenerator4Expression.visit((Expression) columnExpression),
                                                           resultColumn.getAlias());
            } else if (columnExpression instanceof Column) {
                selectExpression.getResultColumnList().add(columnExpression, resultColumn.getAlias());
            } else {
                throw new RuntimeException(StringUtils.format("Invalid field[%s] with type[%s]", resultColumn.toString(), resultColumn.getClass().getName()));
            }
        }

        // Make sure all referenced metrics in field expression are in the sub-query
        FieldExpressionAnalyzer fieldExpressionAnalyzer = new FieldExpressionAnalyzer(this.schema, aggregatedFields, this.sqlDialect);
        this.resultColumns.stream()
                          .filter((f) -> f.getColumnExpression() instanceof Expression)
                          .forEach((f) -> ((Expression) f.getColumnExpression()).getParsedExpression().accept(fieldExpressionAnalyzer));
        for (String metric : fieldExpressionAnalyzer.getMetrics()) {
            subSelectExpression.getResultColumnList().add(metric);
        }
        for (IAggregatableColumn aggregator : fieldExpressionAnalyzer.getWindowFunctionAggregators()) {
            subSelectExpression.getResultColumnList()
                               .add(new StringNode(aggregator.getAggregateExpression().accept(generator)), aggregator.getName());

            // this window fields should be in the group-by clause and select clause,
            // see the javadoc above
            // Use name in the groupBy expression because we have alias for the corresponding field in sub-query expression
            selectExpression.getGroupBy().addField(aggregator.getName());
        }

        //
        // build WhereExpression
        //
        IExpression timestampCol = this.interval.getTimestampColumn();
        Where where = new Where();
        where.addExpression(StringUtils.format("%s >= %s", Expression2Sql.from((String) null, sqlDialect, timestampCol), sqlDialect.formatTimestamp(interval.getStartTime())));
        where.addExpression(StringUtils.format("%s < %s", Expression2Sql.from((String) null, sqlDialect, timestampCol), sqlDialect.formatTimestamp(interval.getEndTime())));
        if (filter != null) {
            where.addExpression(Expression2Sql.from(schema, sqlDialect, filter));
        }

        //
        // build GroupByExpression
        //
        subSelectExpression.getResultColumnList().addAll(groupBy);
        selectExpression.getGroupBy().addFields(groupBy);

        // Make sure all fields in the groupBy are in the field list
        if (!groupBy.isEmpty()) {
            Set<String> existingFields = selectExpression.getResultColumnList().getColumnNames(Collectors.toSet());

            for (String name : groupBy) {
                if (existingFields.add(name)) {
                    IASTNode column = new Column(name);

                    selectExpression.getResultColumnList().add(column);
                    subSelectExpression.getResultColumnList().add(column);
                }
            }
        }

        //
        // build OrderBy/Limit expression
        //
        if (orderBy != null) {
            selectExpression.setOrderBy(new OrderBy(orderBy.getName(), orderBy.getOrder()));
        }
        if (limit != null) {
            selectExpression.setLimit(new Limit(limit.getLimit(), limit.getOffset()));
        }

        //
        // Link query and subQuery together
        //
        if (hasSubSelect) {
            subSelectExpression.getFrom().setExpression(new Table(sqlTableName));
            subSelectExpression.setWhere(where);
            selectExpression.getFrom().setExpression(subSelectExpression);

            // For MySQL, the sub-query must have an alias
            selectExpression.getFrom().setAlias(new ColumnAlias("nest"));
        } else {
            selectExpression.getFrom().setExpression(new Table(sqlTableName));
            selectExpression.setWhere(where);
        }
        return selectExpression;
    }
}
