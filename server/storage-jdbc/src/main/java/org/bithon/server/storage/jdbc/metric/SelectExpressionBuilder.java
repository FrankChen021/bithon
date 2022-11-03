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
import org.bithon.server.storage.datasource.api.IQueryStageAggregator;
import org.bithon.server.storage.datasource.api.QueryStageAggregators;
import org.bithon.server.storage.datasource.query.ast.FieldExpressionVisitorAdaptor2;
import org.bithon.server.storage.datasource.spec.IMetricSpec;
import org.bithon.server.storage.datasource.spec.MetricSpecVisitorAdaptor;
import org.bithon.server.storage.datasource.spec.PostAggregatorMetricSpec;
import org.bithon.server.storage.jdbc.dsl.sql.GroupByExpression;
import org.bithon.server.storage.jdbc.dsl.sql.LimitExpression;
import org.bithon.server.storage.jdbc.dsl.sql.NameExpression;
import org.bithon.server.storage.jdbc.dsl.sql.OrderByExpression;
import org.bithon.server.storage.jdbc.dsl.sql.SelectExpression;
import org.bithon.server.storage.jdbc.dsl.sql.StringExpression;
import org.bithon.server.storage.jdbc.dsl.sql.TableExpression;
import org.bithon.server.storage.jdbc.dsl.sql.WhereExpression;
import org.bithon.server.storage.jdbc.utils.SQLFilterBuilder;
import org.bithon.server.storage.metrics.IFilter;
import org.bithon.server.storage.metrics.Interval;
import org.bithon.server.storage.metrics.Limit;
import org.bithon.server.storage.metrics.OrderBy;

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

    private Collection<Object> fields;

    private Collection<IFilter> filters;
    private Interval interval;

    private List<String> groupBy;

    @Nullable
    private OrderBy orderBy;

    @Nullable
    private Limit limit;

    private ISqlDialect sqlDialect;

    public static SelectExpressionBuilder builder() {
        return new SelectExpressionBuilder();
    }

    public SelectExpressionBuilder dataSource(DataSourceSchema dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    public SelectExpressionBuilder fields(Collection<Object> fields) {
        this.fields = fields;
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

    public SelectExpressionBuilder orderBy(@Nullable OrderBy orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public SelectExpressionBuilder limit(@Nullable Limit limit) {
        this.limit = limit;
        return this;
    }

    public SelectExpressionBuilder sqlDialect(ISqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
        return this;
    }

    static class FieldExpressionAnalyzer extends FieldExpressionVisitorAdaptor2 {

        private final Map<String, IQueryStageAggregator> preAggregators;
        private final ISqlDialect sqlExpressionFormatter;

        @Getter
        private final List<IQueryStageAggregator> windowFunctionAggregators = new ArrayList<>();

        @Getter
        private final Set<String> metrics = new HashSet<>();
        private final DataSourceSchema schema;

        FieldExpressionAnalyzer(DataSourceSchema schema,
                                Map<String, IQueryStageAggregator> queryStageAggregators,
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

            if (sqlExpressionFormatter.useWindowFunctionAsAggregator(metricSpec.getQueryAggregator())) {
                // The aggregator uses WindowFunction, it will be in a sub-query of generated SQL
                // So, we turn the metric into a pre-aggregator
                preAggregators.put(metricSpec.getName(), metricSpec.getQueryAggregator());

                windowFunctionAggregators.add(metricSpec.getQueryAggregator());
            } else {
                // this metric should also be in the sub-query expression
                metrics.add(metricSpec.getName());
            }
        }
    }

    static class FieldExpressionSQLGenerator extends MetricSpecVisitorAdaptor<StringExpression> {
        private final ISqlDialect sqlDialect;
        private final QueryStageAggregatorSQLGenerator queryStageAggregatorSQLGenerator;

        protected final Map<String, Object> internalVariables;
        private final Map<String, IQueryStageAggregator> existingAggregators;

        private final DataSourceSchema schema;

        FieldExpressionSQLGenerator(DataSourceSchema schema,
                                    ISqlDialect sqlDialect,
                                    Map<String, IQueryStageAggregator> existingAggregators,
                                    QueryStageAggregatorSQLGenerator queryStageAggregatorSQLGenerator,
                                    Map<String, Object> internalVariables) {
            this.schema = schema;
            this.sqlDialect = sqlDialect;
            this.existingAggregators = existingAggregators;
            this.queryStageAggregatorSQLGenerator = queryStageAggregatorSQLGenerator;
            this.internalVariables = internalVariables;
        }

        @Override
        public StringExpression visit(PostAggregatorMetricSpec metricSpec) {

            final StringBuilder sb = new StringBuilder(32);
            metricSpec.visitExpression(new FieldExpressionVisitorAdaptor2() {

                @Override
                public void visitField(IColumnSpec columnSpec) {

                    IMetricSpec metricSpec = (IMetricSpec) columnSpec;

                    // Case 1. The field used in window function is presented in a sub-query, at the root query level we only reference the name
                    boolean useWindowFunctionAsAggregator = sqlDialect.useWindowFunctionAsAggregator(metricSpec.getQueryAggregator());

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

            sb.append(StringUtils.format(" AS \"%s\"", metricSpec.getName()));

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
    public SelectExpression build() {
        String sqlTableName = "bithon_" + dataSource.getName().replace("-", "_");

        //
        // Turn some metrics(those use window functions for aggregation) in expression into pre-aggregator first
        //
        Map<String, IQueryStageAggregator> existingAggregators = this.fields.stream()
                                                                            .filter((f) -> f instanceof IQueryStageAggregator)
                                                                            .collect(Collectors.toMap((f) -> ((IQueryStageAggregator) f).getName(),
                                                                                                      (v) -> (IQueryStageAggregator) v));

        SelectExpression selectExpression = new SelectExpression();
        selectExpression.setGroupBy(new GroupByExpression());
        SelectExpression subSelectExpression = new SelectExpression();

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
                                                                                                  generator.noAlias(),
                                                                                                  ImmutableMap.of("interval",
                                                                                                                  interval.getStep(),
                                                                                                                  "instanceCount",
                                                                                                                  "count(distinct \"instanceName\")"));

        //for (IQueryStageAggregator aggregator : this.aggregators.values()) {
        for (Object field : this.fields) {
            if (field instanceof IQueryStageAggregator) {
                IQueryStageAggregator aggregator = (IQueryStageAggregator) field;

                // if window function is contained, the final SQL has a sub-query
                if (sqlDialect.useWindowFunctionAsAggregator(aggregator)) {
                    subSelectExpression.getFieldsExpression().addField(new StringExpression(aggregator.accept(generator)));

                    // this window fields should be in the group-by clause and select clause,
                    // see the javadoc above
                    // Use name in the groupBy expression because we have alias for corresponding field in sub-query expression
                    selectExpression.getGroupBy().addField(aggregator.getName());
                    selectExpression.getFieldsExpression().addField(new NameExpression(aggregator.getName()));

                    hasSubSelect = true;
                } else {
                    selectExpression.getFieldsExpression().addField(new StringExpression(aggregator.accept(generator)));

                    // Special case for some aggregators, they must also be grouped in sub-query
                    // for cardinality, we put it here instead of in `convertToGroupByQuery`
                    // because in some cases, this operator don't need to be in the groupBy expression which is constructed in that method
                    if (aggregator.getType().equals(QueryStageAggregators.CardinalityAggregator.TYPE)) {
                        selectExpression.getGroupBy().addField(aggregator.getField());
                    }

                    // This metric should also be in the sub-query, see the example in the javadoc above
                    subSelectExpression.getFieldsExpression().addField(new NameExpression(aggregator.getField()));
                }
            } else if (field instanceof PostAggregatorMetricSpec) {
                selectExpression.getFieldsExpression().addField(((PostAggregatorMetricSpec) field).accept(fieldExpressionSQLGenerator));
            } else if (field instanceof String) {
                selectExpression.getFieldsExpression().addField(new NameExpression((String) field));
            } else {
                throw new RuntimeException(StringUtils.format("Invalid field[%s] with type[%s]", field.toString(), field.getClass().getName()));
            }
        }

        // Make sure all referenced metrics in field expression are in the sub-query
        FieldExpressionAnalyzer fieldExpressionAnalyzer = new FieldExpressionAnalyzer(this.dataSource, existingAggregators, this.sqlDialect);
        this.fields.stream()
                   .filter((f) -> f instanceof PostAggregatorMetricSpec)
                   .forEach((postAggregator) -> ((PostAggregatorMetricSpec) postAggregator).visitExpression(fieldExpressionAnalyzer));
        for (String metric : fieldExpressionAnalyzer.getMetrics()) {
            subSelectExpression.getFieldsExpression().addField(new NameExpression(metric));
        }
        for (IQueryStageAggregator aggregator : fieldExpressionAnalyzer.getWindowFunctionAggregators()) {
            subSelectExpression.getFieldsExpression().addField(new StringExpression(aggregator.accept(generator)));

            // this window fields should be in the group-by clause and select clause,
            // see the javadoc above
            // Use name in the groupBy expression because we have alias for corresponding field in sub-query expression
            selectExpression.getGroupBy().addField(aggregator.getName());
        }

        //
        // build WhereExpression
        //
        WhereExpression whereExpression = new WhereExpression();
        whereExpression.addExpression(StringUtils.format("\"timestamp\" >= %s", sqlDialect.formatTimestamp(interval.getStartTime())));
        whereExpression.addExpression(StringUtils.format("\"timestamp\" < %s", sqlDialect.formatTimestamp(interval.getEndTime())));
        for (IFilter filter : filters) {
            whereExpression.addExpression(filter.getMatcher().accept(new SQLFilterBuilder(dataSource, filter)));
        }

        //
        // build GroupByExpression
        //
        subSelectExpression.getFieldsExpression().addFields(groupBy);
        selectExpression.getGroupBy().addFields(groupBy);

        //
        // build OrderBy/Limit expression
        //
        if (orderBy != null) {
            selectExpression.setOrderBy(new OrderByExpression(orderBy.getName(), orderBy.getOrder()));
        }
        if (limit != null) {
            selectExpression.setLimit(new LimitExpression(limit.getLimit(), limit.getOffset()));
        }

        //
        // Link query and subQuery together
        //
        if (hasSubSelect) {
            subSelectExpression.getFrom().setExpression(new TableExpression(sqlTableName));
            subSelectExpression.setWhere(whereExpression);
            selectExpression.getFrom().setExpression(subSelectExpression);
        } else {
            selectExpression.getFrom().setExpression(new TableExpression(sqlTableName));
            selectExpression.setWhere(whereExpression);
        }
        return selectExpression;
    }
}
