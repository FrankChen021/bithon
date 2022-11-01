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
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.api.IQueryStageAggregator;
import org.bithon.server.storage.datasource.spec.IMetricSpec;
import org.bithon.server.storage.datasource.spec.MetricSpecVisitorAdaptor;
import org.bithon.server.storage.datasource.spec.PostAggregatorExpressionVisitor;
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
import java.util.Collections;
import java.util.HashMap;
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

    private List<String> metrics;
    private Map<String, IQueryStageAggregator> aggregators;

    /**
     * This is an optional field, so a default value is needed.
     */
    private List<PostAggregatorMetricSpec> postAggregators = Collections.emptyList();
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

    public SelectExpressionBuilder metrics(List<String> metrics) {
        this.metrics = metrics;
        return this;
    }

    public SelectExpressionBuilder aggregators(List<IQueryStageAggregator> aggregators) {
        // Create a new instance for empty aggregators because they might be modified during building
        this.aggregators = CollectionUtils.isEmpty(aggregators) ?
                           new HashMap<>(4) :
                           aggregators.stream().collect(Collectors.toMap(IQueryStageAggregator::getName, (agg) -> agg));
        return this;
    }

    public SelectExpressionBuilder postAggregators(List<PostAggregatorMetricSpec> postAggregators) {
        this.postAggregators = postAggregators;
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

    static class PreAggregatorExtractor implements PostAggregatorExpressionVisitor {

        private final Map<String, IQueryStageAggregator> preAggregators;
        private final ISqlDialect sqlExpressionFormatter;

        @Getter
        private final Set<String> metrics = new HashSet<>();

        PreAggregatorExtractor(Map<String, IQueryStageAggregator> queryStageAggregators, ISqlDialect sqlFormatter) {
            this.preAggregators = queryStageAggregators;
            this.sqlExpressionFormatter = sqlFormatter;
        }

        @Override
        public void visitMetric(IMetricSpec metricSpec) {
            if (preAggregators.containsKey(metricSpec.getName())) {
                return;
            }

            if (sqlExpressionFormatter.useWindowFunctionAsAggregator(metricSpec.getQueryAggregator())) {
                // The aggregator uses WindowFunction, it will be in a sub-query of generated SQL
                // So, we turn the metric into a pre-aggregator
                preAggregators.put(metricSpec.getName(), metricSpec.getQueryAggregator());
            } else {
                // this metric should also be in the sub-query expression
                metrics.add(metricSpec.getName());
            }
        }

        @Override
        public void visitConstant(String number) {
        }

        @Override
        public void visitorOperator(String operator) {
        }

        @Override
        public void beginSubExpression() {
        }

        @Override
        public void endSubExpression() {
        }

        @Override
        public void visitVariable(String variable) {
        }
    }

    static class PostAggregatorExpressionGenerator extends MetricSpecVisitorAdaptor<StringExpression> {
        private final ISqlDialect sqlDialect;
        private final QueryStageAggregatorSQLGenerator queryStageAggregatorSQLGenerator;

        protected final Map<String, Object> internalVariables;
        private final Map<String, IQueryStageAggregator> preAggregators;

        PostAggregatorExpressionGenerator(ISqlDialect sqlDialect,
                                          Map<String, IQueryStageAggregator> preAggregators,
                                          QueryStageAggregatorSQLGenerator queryStageAggregatorSQLGenerator,
                                          Map<String, Object> internalVariables) {
            this.sqlDialect = sqlDialect;
            this.preAggregators = preAggregators;
            this.queryStageAggregatorSQLGenerator = queryStageAggregatorSQLGenerator;
            this.internalVariables = internalVariables;
        }

        @Override
        public StringExpression visit(PostAggregatorMetricSpec metricSpec) {

            final StringBuilder sb = new StringBuilder(32);
            metricSpec.visitExpression(new PostAggregatorExpressionVisitor() {

                @Override
                public void visitMetric(IMetricSpec metricSpec) {
                    // Case 1. The field used in window function is presented in a sub-query, at the root query level we only reference the name
                    boolean useWindowFunctionAsAggregator = sqlDialect.useWindowFunctionAsAggregator(metricSpec.getQueryAggregator());

                    // Case 2. Some DB does not allow same aggregation expressions, we use the existing expression
                    boolean hasSameExpression = !sqlDialect.allowSameAggregatorExpression() && preAggregators.containsKey(metricSpec.getName());

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
        // To compatible with old interface, turn metric list into (pre/post)aggregators.
        // Needs to be cleaned in the future.
        //
        if (CollectionUtils.isNotEmpty(metrics)) {

            // turn input aggregators into mutable collection first since it might be modified
            postAggregators = new ArrayList<>(postAggregators);

            for (String metric : metrics) {
                IMetricSpec metricSpec = dataSource.getMetricSpecByName(metric);
                if (metricSpec == null) {
                    throw new RuntimeException(StringUtils.format("metric[%s] does not exist.", metric));
                }
                if (metricSpec instanceof PostAggregatorMetricSpec) {
                    postAggregators.add((PostAggregatorMetricSpec) metricSpec);
                } else {
                    aggregators.put(metricSpec.getName(), metricSpec.getQueryAggregator());
                }
            }
        }

        //
        // Turn some metrics in expression into pre-aggregator first
        //
        PreAggregatorExtractor postAggregatorExtractor = new PreAggregatorExtractor(this.aggregators, this.sqlDialect);
        for (PostAggregatorMetricSpec postAggregator : postAggregators) {
            postAggregator.visitExpression(postAggregatorExtractor);
        }

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
        for (IQueryStageAggregator aggregator : this.aggregators.values()) {
            // if window function is contained, the final SQL has a sub-query
            if (sqlDialect.useWindowFunctionAsAggregator(aggregator)) {
                subSelectExpression.getFieldsExpression().addField(new StringExpression(aggregator.accept(generator)));

                // this window fields should be in the group-by clause and select clause,
                // see the javadoc above
                selectExpression.getGroupBy().addField(aggregator.getName());
                selectExpression.getFieldsExpression().addField(new NameExpression(aggregator.getName()));

                hasSubSelect = true;
            } else {
                selectExpression.getFieldsExpression().addField(new StringExpression(aggregator.accept(generator)));

                // This metric should also be in the sub-query, see the example in the javadoc above
                subSelectExpression.getFieldsExpression().addField(new NameExpression(aggregator.getName()));
            }
        }

        // Make sure all referenced metrics in expression are in the sub-query
        for (String metric : postAggregatorExtractor.getMetrics()) {
            subSelectExpression.getFieldsExpression().addField(new NameExpression(metric));
        }

        // Generate expression for post aggregators
        if (!CollectionUtils.isEmpty(postAggregators)) {
            Map<String, Object> internalVariables = ImmutableMap.of("interval",
                                                                    interval.getStep(),
                                                                    "instanceCount",
                                                                    "count(distinct \"instanceName\")");
            PostAggregatorExpressionGenerator postAggregatorExpressionGenerator = new PostAggregatorExpressionGenerator(sqlDialect,
                                                                                                                        aggregators,
                                                                                                                        generator.noAlias(),
                                                                                                                        internalVariables);

            for (PostAggregatorMetricSpec postAggregator : postAggregators) {
                selectExpression.getFieldsExpression().addField(postAggregator.accept(postAggregatorExpressionGenerator));
            }
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
        selectExpression.getFieldsExpression().addFields(groupBy);
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
