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
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.IColumnSpec;
import org.bithon.server.storage.datasource.query.ast.Column;
import org.bithon.server.storage.datasource.query.ast.Expression;
import org.bithon.server.storage.datasource.query.ast.GroupBy;
import org.bithon.server.storage.datasource.query.ast.IAST;
import org.bithon.server.storage.datasource.query.ast.Limit;
import org.bithon.server.storage.datasource.query.ast.Name;
import org.bithon.server.storage.datasource.query.ast.OrderBy;
import org.bithon.server.storage.datasource.query.ast.ResultColumn;
import org.bithon.server.storage.datasource.query.ast.ResultColumnList;
import org.bithon.server.storage.datasource.query.ast.SelectStatement;
import org.bithon.server.storage.datasource.query.ast.SimpleAggregator;
import org.bithon.server.storage.datasource.query.ast.SimpleAggregators;
import org.bithon.server.storage.datasource.query.ast.StringExpression;
import org.bithon.server.storage.datasource.query.ast.Table;
import org.bithon.server.storage.datasource.query.ast.Where;
import org.bithon.server.storage.datasource.query.parser.FieldExpressionVisitorAdaptor2;
import org.bithon.server.storage.datasource.spec.IMetricSpec;
import org.bithon.server.storage.jdbc.utils.SQLFilterBuilder;
import org.bithon.server.storage.metrics.IFilter;
import org.bithon.server.storage.metrics.Interval;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
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

    private DataSourceSchema dataSource;

    private ResultColumnList resultColumnList;

    private Collection<IFilter> filters;
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

    public SelectExpressionBuilder dataSource(DataSourceSchema dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    public SelectExpressionBuilder fields(ResultColumnList resultColumnList) {
        this.resultColumnList = resultColumnList;
        return this;
    }

    public SelectExpressionBuilder filters(Collection<IFilter> filters) {
        this.filters = filters;
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

        private final Map<String, Object> preAggregators;
        private final ISqlDialect sqlExpressionFormatter;

        @Getter
        private final List<IMetricSpec> windowFunctionAggregators = new ArrayList<>();

        @Getter
        private final Set<String> metrics = new HashSet<>();
        private final DataSourceSchema schema;

        FieldExpressionAnalyzer(DataSourceSchema schema,
                                Map<String, Object> queryStageAggregators,
                                ISqlDialect sqlFormatter) {
            this.schema = schema;
            this.preAggregators = queryStageAggregators;
            this.sqlExpressionFormatter = sqlFormatter;
        }

        @Override
        protected DataSourceSchema getSchema() {
            return schema;
        }

        @Override
        public void visitField(IColumnSpec columnSpec) {
            if (!(columnSpec instanceof IMetricSpec)) {
                throw new RuntimeException(StringUtils.format("field [%s] is not a metric", columnSpec.getName()));
            }

            IMetricSpec metricSpec = (IMetricSpec) columnSpec;
            if (preAggregators.containsKey(metricSpec.getName())) {
                return;
            }

            if (sqlExpressionFormatter.useWindowFunctionAsAggregator(metricSpec.getQueryAggregator().getType())) {
                // The aggregator uses WindowFunction, it will be in a sub-query of generated SQL
                // So, we turn the metric into a pre-aggregator
                preAggregators.put(metricSpec.getName(), metricSpec.getQueryAggregator());

                windowFunctionAggregators.add(metricSpec);
            } else {
                // this metric should also be in the sub-query expression
                metrics.add(metricSpec.getName());
            }
        }
    }

    static class FieldExpressionSQLGenerator {
        private final ISqlDialect sqlDialect;
        private final QueryStageAggregatorSQLGenerator queryStageAggregatorSQLGenerator;

        protected final Map<String, Object> internalVariables;
        private final Map<String, Object> existingAggregators;

        private final DataSourceSchema schema;

        FieldExpressionSQLGenerator(DataSourceSchema schema,
                                    ISqlDialect sqlDialect,
                                    Map<String, Object> existingAggregators,
                                    QueryStageAggregatorSQLGenerator queryStageAggregatorSQLGenerator,
                                    Map<String, Object> internalVariables) {
            this.schema = schema;
            this.sqlDialect = sqlDialect;
            this.existingAggregators = existingAggregators;
            this.queryStageAggregatorSQLGenerator = queryStageAggregatorSQLGenerator;
            this.internalVariables = internalVariables;
        }

        public StringExpression visit(Expression expression) {

            final StringBuilder sb = new StringBuilder(32);
            expression.visitExpression(new FieldExpressionVisitorAdaptor2() {

                @Override
                public void visitField(IColumnSpec columnSpec) {

                    IMetricSpec metricSpec = (IMetricSpec) columnSpec;

                    // Case 1. The field used in window function is presented in a sub-query, at the root query level we only reference the name
                    boolean useWindowFunctionAsAggregator = sqlDialect.useWindowFunctionAsAggregator(metricSpec.getQueryAggregator().getType());

                    // Case 2. Some DB does not allow same aggregation expressions, we use the existing expression
                    boolean hasSameExpression = !sqlDialect.allowSameAggregatorExpression() && existingAggregators.containsKey(metricSpec.getName());

                    if (useWindowFunctionAsAggregator || hasSameExpression) {
                        sb.append('"');
                        sb.append(metricSpec.getName());
                        sb.append('"');
                    } else {
                        // generate a aggregation expression
                        sb.append(metricSpec.getQueryAggregator().accept(queryStageAggregatorSQLGenerator));
                    }
                }

                @Override
                protected DataSourceSchema getSchema() {
                    return schema;
                }

                @Override
                public void visitConstant(String number) {
                    sb.append(number);
                }

                @Override
                public void visitorOperator(String operator) {
                    sb.append(operator);
                }

                @Override
                public void beginSubExpression() {
                    sb.append('(');
                }

                @Override
                public void endSubExpression() {
                    sb.append(')');
                }

                @Override
                public void visitVariable(String variable) {
                    Object variableValue = internalVariables.get(variable);
                    if (variableValue == null) {
                        throw new RuntimeException(StringUtils.format("variable (%s) not provided in context",
                                                                      variable));
                    }
                    sb.append(variableValue);
                }

                @Override
                public void beginFunction(String name) {
                    sb.append(name);
                    sb.append('(');
                }

                @Override
                public void endFunction() {
                    sb.append(')');
                }

                @Override
                public void endFunctionArgument(int argIndex, int count) {
                    if (argIndex < count - 1) {
                        sb.append(',');
                    }
                }
            });

            return new StringExpression(sb.toString());
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
    public SelectStatement build() {
        String sqlTableName = "bithon_" + dataSource.getName().replace("-", "_");

        //
        // Turn some metrics(those use window functions for aggregation) in expression into pre-aggregator first
        //
        Map<String, Object> existingAggregators = this.resultColumnList.columnStream()
                                                                       .filter((f) -> f.getColumnExpression() instanceof SimpleAggregator)
                                                                       .collect(Collectors.toMap((f) -> ((SimpleAggregator) f.getColumnExpression()).getTargetField(),
                                                                                                 (v) -> v));

        SelectStatement selectStatement = new SelectStatement();
        selectStatement.setGroupBy(new GroupBy());
        SelectStatement subSelectStatement = new SelectStatement();

        //
        // fields
        //
        boolean hasSubSelect = false;
        QueryStageAggregatorSQLGenerator generator = new QueryStageAggregatorSQLGenerator(sqlDialect,
                                                                                          interval.getTotalLength(),
                                                                                          interval.getStep());

        FieldExpressionSQLGenerator fieldExpressionSQLGenerator = new FieldExpressionSQLGenerator(dataSource,
                                                                                                  sqlDialect,
                                                                                                  existingAggregators,
                                                                                                  generator,
                                                                                                  ImmutableMap.of("interval",
                                                                                                                  interval.getStep(),
                                                                                                                  "instanceCount",
                                                                                                                  "count(distinct \"instanceName\")"));

        for (ResultColumn resultColumn : this.resultColumnList.getColumns()) {
            IAST fieldExpr = resultColumn.getColumnExpression();
            if (fieldExpr instanceof SimpleAggregator) {
                SimpleAggregator function = (SimpleAggregator) fieldExpr;

                // if window function is contained, the final SQL has a sub-query
                if (sqlDialect.useWindowFunctionAsAggregator(function.getFnName())) {
                    subSelectStatement.getResultColumnList().add(new StringExpression(function.accept(generator)), resultColumn.getAlias());

                    // this window fields should be in the group-by clause and select clause,
                    // see the javadoc above
                    // Use name in the groupBy expression because we have alias for corresponding field in sub-query expression
                    selectStatement.getGroupBy().addField(resultColumn.getAlias().getName());
                    selectStatement.getResultColumnList().add(resultColumn.getAlias().getName());

                    hasSubSelect = true;
                } else {
                    selectStatement.getResultColumnList().add(new StringExpression(function.accept(generator)), resultColumn.getAlias());

                    String underlyingFieldName = ((Name) function.getArguments().get(0)).getName();

                    // Special case for some aggregators, they must also be grouped in sub-query
                    // for cardinality, we put it here instead of in `convertToGroupByQuery`
                    // because in some cases, this operator don't need to be in the groupBy expression which is constructed in that method
                    if (function.getFnName().equals(SimpleAggregators.CardinalityAggregator.TYPE)) {
                        selectStatement.getGroupBy().addField(underlyingFieldName);
                    }

                    // This metric should also be in the sub-query, see the example in the javadoc above
                    subSelectStatement.getResultColumnList().add(underlyingFieldName);
                }
            } else if (fieldExpr instanceof Expression) {
                selectStatement.getResultColumnList().add(fieldExpressionSQLGenerator.visit((Expression) fieldExpr),
                                                          resultColumn.getAlias());
            } else if (fieldExpr instanceof Column) {
                selectStatement.getResultColumnList().add(fieldExpr);
            } else {
                throw new RuntimeException(StringUtils.format("Invalid field[%s] with type[%s]", resultColumn.toString(), resultColumn.getClass().getName()));
            }
        }

        // Make sure all referenced metrics in field expression are in the sub-query
        FieldExpressionAnalyzer fieldExpressionAnalyzer = new FieldExpressionAnalyzer(this.dataSource, existingAggregators, this.sqlDialect);
        this.resultColumnList.columnStream()
                             .filter((f) -> f.getColumnExpression() instanceof Expression)
                             .forEach((f) -> ((Expression) f.getColumnExpression()).visitExpression(fieldExpressionAnalyzer));
        for (String metric : fieldExpressionAnalyzer.getMetrics()) {
            subSelectStatement.getResultColumnList().add(metric);
        }
        for (IMetricSpec aggregator : fieldExpressionAnalyzer.getWindowFunctionAggregators()) {
            subSelectStatement.getResultColumnList()
                              .add(new StringExpression(aggregator.getQueryAggregator().accept(generator)), aggregator.getName());

            // this window fields should be in the group-by clause and select clause,
            // see the javadoc above
            // Use name in the groupBy expression because we have alias for corresponding field in sub-query expression
            selectStatement.getGroupBy().addField(aggregator.getName());
        }

        //
        // build WhereExpression
        //
        Where where = new Where();
        where.addExpression(StringUtils.format("\"timestamp\" >= %s", sqlDialect.formatTimestamp(interval.getStartTime())));
        where.addExpression(StringUtils.format("\"timestamp\" < %s", sqlDialect.formatTimestamp(interval.getEndTime())));
        for (IFilter filter : filters) {
            where.addExpression(filter.getMatcher().accept(new SQLFilterBuilder(dataSource, filter)));
        }

        //
        // build GroupByExpression
        //
        subSelectStatement.getResultColumnList().addAll(groupBy);
        selectStatement.getGroupBy().addFields(groupBy);

        // Make sure all fields in the groupBy are in the fields list
        if (!groupBy.isEmpty()) {
            Set<String> existingFields = selectStatement.getResultColumnList().getColumnNames(Collectors.toSet());

            for (String name : groupBy) {
                if (existingFields.add(name)) {
                    IAST column = new Column(name);

                    selectStatement.getResultColumnList().add(column);
                    subSelectStatement.getResultColumnList().add(column);
                }
            }
        }


        //
        // build OrderBy/Limit expression
        //
        if (orderBy != null) {
            selectStatement.setOrderBy(new OrderBy(orderBy.getName(), orderBy.getOrder()));
        }
        if (limit != null) {
            selectStatement.setLimit(new Limit(limit.getLimit(), limit.getOffset()));
        }

        //
        // Link query and subQuery together
        //
        if (hasSubSelect) {
            subSelectStatement.getFrom().setExpression(new Table(sqlTableName));
            subSelectStatement.setWhere(where);
            selectStatement.getFrom().setExpression(subSelectStatement);
        } else {
            selectStatement.getFrom().setExpression(new Table(sqlTableName));
            selectStatement.setWhere(where);
        }
        return selectStatement;
    }
}
