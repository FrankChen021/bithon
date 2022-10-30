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
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.api.IQueryStageAggregator;
import org.bithon.server.storage.datasource.spec.IMetricSpec;
import org.bithon.server.storage.datasource.spec.PostAggregatorMetricSpec;
import org.bithon.server.storage.jdbc.dsl.sql.GroupByExpression;
import org.bithon.server.storage.jdbc.dsl.sql.NameExpression;
import org.bithon.server.storage.jdbc.dsl.sql.OrderByExpression;
import org.bithon.server.storage.jdbc.dsl.sql.SelectExpression;
import org.bithon.server.storage.jdbc.dsl.sql.StringExpression;
import org.bithon.server.storage.jdbc.dsl.sql.TableExpression;
import org.bithon.server.storage.jdbc.dsl.sql.WhereExpression;
import org.bithon.server.storage.jdbc.utils.SQLFilterBuilder;
import org.bithon.server.storage.metrics.IFilter;
import org.bithon.server.storage.metrics.Interval;
import org.bithon.server.storage.metrics.OrderBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/10/30 11:52
 */
public class SelectExpressionBuilder {

    /**
     * Need to be optimized to generate ASTExpression directly
     */
    interface IMetricFieldsClauseBuilderSupplier {
        MetricJdbcReader.MetricFieldsClauseBuilder create(String tableName,
                                                          DataSourceSchema dataSource,
                                                          Set<String> existingAggregators,
                                                          Map<String, Object> variables);
    }

    private DataSourceSchema dataSource;

    private List<String> metrics;
    private List<IQueryStageAggregator> aggregators;

    private List<PostAggregatorMetricSpec> postAggregators;
    private Collection<IFilter> filters;
    private Interval interval;

    private List<String> groupBy;
    private OrderBy orderBy;

    private ISqlExpressionFormatter sqlFormatter;

    private IMetricFieldsClauseBuilderSupplier metricFieldsClauseBuilderSupplier;

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
        this.aggregators = aggregators;
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

    public SelectExpressionBuilder orderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public SelectExpressionBuilder sqlFormatter(ISqlExpressionFormatter sqlFormatter) {
        this.sqlFormatter = sqlFormatter;
        return this;
    }

    public SelectExpressionBuilder metricFieldsClauseBuilderSupplier(IMetricFieldsClauseBuilderSupplier metricFieldsClauseBuilderSupplier) {
        this.metricFieldsClauseBuilderSupplier = metricFieldsClauseBuilderSupplier;
        return this;
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

        List<IQueryStageAggregator> aggregatorList = new ArrayList<>(aggregators);
        //
        // To compatible with old interface, turn metric list into aggregators
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
                    aggregatorList.add(metricSpec.getQueryAggregator());
                }
            }
        }

        //
        // First, check if fields that are referenced in post aggregators are listed in QueryStageAggregators(pre-aggregator)
        //
        for (PostAggregatorMetricSpec postMetricSpec : postAggregators) {

        }

        SelectExpression selectExpression = new SelectExpression();
        selectExpression.setGroupBy(new GroupByExpression());
        SelectExpression subSelectExpression = new SelectExpression();

        Set<String> aggregatorExpressions = new HashSet<>();

        //
        // fields
        //
        boolean hasSubSelect = false;
        QueryStageAggregatorSQLGenerator generator = new QueryStageAggregatorSQLGenerator(sqlFormatter,
                                                                                          interval.getTotalLength(),
                                                                                          interval.getStep());
        for (IQueryStageAggregator aggregator : aggregatorList) {
            // if window function is contained, the final SQL has a sub-query
            if (sqlFormatter.useWindowFunctionAsAggregator(aggregator)) {
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

                aggregatorExpressions.add(StringUtils.format("%s(\"%s\")", aggregator.getType(), aggregator.getField()));
            }
        }

        // post aggregators
        if (!CollectionUtils.isEmpty(postAggregators)) {
            MetricJdbcReader.MetricFieldsClauseBuilder metricFieldsBuilder = this.metricFieldsClauseBuilderSupplier.create(sqlTableName,
                                                                                                                           dataSource,
                                                                                                                           aggregatorExpressions,
                                                                                                                           ImmutableMap.of("interval",
                                                                                                                                           interval.getStep(),
                                                                                                                                           "instanceCount",
                                                                                                                                           "count(distinct \"instanceName\")"));

            for (PostAggregatorMetricSpec postMetricSpec : postAggregators) {
                selectExpression.getFieldsExpression().addField(new StringExpression(postMetricSpec.accept(metricFieldsBuilder)));
            }
        }

        //
        // build WhereExpression
        //
        WhereExpression whereExpression = new WhereExpression();
        whereExpression.addExpression(StringUtils.format("\"timestamp\" >= %s", sqlFormatter.formatTimestamp(interval.getStartTime())));
        whereExpression.addExpression(StringUtils.format("\"timestamp\" < %s", sqlFormatter.formatTimestamp(interval.getEndTime())));
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
        // build OrderByExpression
        //
        if (orderBy != null) {
            selectExpression.setOrderBy(new OrderByExpression(orderBy.getName(), orderBy.getOrder()));
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
