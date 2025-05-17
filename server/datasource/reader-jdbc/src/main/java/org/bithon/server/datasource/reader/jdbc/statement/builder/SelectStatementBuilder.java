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

package org.bithon.server.datasource.reader.jdbc.statement.builder;

import jakarta.annotation.Nullable;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionInDepthVisitor;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.component.commons.expression.optimzer.AbstractOptimizer;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.TimestampSpec;
import org.bithon.server.datasource.column.ExpressionColumn;
import org.bithon.server.datasource.column.IColumn;
import org.bithon.server.datasource.query.Interval;
import org.bithon.server.datasource.query.ast.Column;
import org.bithon.server.datasource.query.ast.ExpressionNode;
import org.bithon.server.datasource.query.ast.IASTNode;
import org.bithon.server.datasource.query.ast.Selector;
import org.bithon.server.datasource.query.setting.QuerySettings;
import org.bithon.server.datasource.reader.jdbc.dialect.ISqlDialect;
import org.bithon.server.datasource.reader.jdbc.statement.ast.HavingClause;
import org.bithon.server.datasource.reader.jdbc.statement.ast.LimitClause;
import org.bithon.server.datasource.reader.jdbc.statement.ast.OrderByClause;
import org.bithon.server.datasource.reader.jdbc.statement.ast.OrderByElement;
import org.bithon.server.datasource.reader.jdbc.statement.ast.QueryStageFunctions;
import org.bithon.server.datasource.reader.jdbc.statement.ast.SelectStatement;
import org.bithon.server.datasource.reader.jdbc.statement.ast.TableIdentifier;
import org.bithon.server.datasource.reader.jdbc.statement.ast.TextNode;
import org.bithon.server.datasource.reader.jdbc.statement.ast.WindowFunctionExpression;
import org.bithon.server.datasource.reader.jdbc.statement.ast.WindowFunctionFrame;
import org.bithon.server.datasource.reader.jdbc.statement.serializer.Expression2Sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/10/30 11:52
 */
public class SelectStatementBuilder {

    private ISchema schema;

    private List<Selector> selectors;
    private IExpression filter;
    private Interval interval;
    private List<String> groupBy = Collections.emptyList();

    @Nullable
    private OrderByClause[] orderBys;

    @Nullable
    private org.bithon.server.datasource.query.Limit limit;

    @Nullable
    private HumanReadableDuration offset;

    private ISqlDialect sqlDialect;
    private QuerySettings querySettings = QuerySettings.DEFAULT;

    public boolean hasSlidingWindowAggregation() {
        return interval.getWindow() != null &&
               interval.getStep() != null
               && interval.getWindow().getDuration().getSeconds() > interval.getStep().getSeconds();
    }

    public static SelectStatementBuilder builder() {
        return new SelectStatementBuilder();
    }

    public SelectStatementBuilder schema(ISchema schema) {
        this.schema = schema;
        return this;
    }

    public SelectStatementBuilder fields(List<Selector> selectors) {
        this.selectors = new ArrayList<>(selectors);
        return this;
    }

    public SelectStatementBuilder filter(IExpression filter) {
        this.filter = filter;
        return this;
    }

    public SelectStatementBuilder interval(Interval interval) {
        this.interval = interval;
        return this;
    }

    public SelectStatementBuilder groupBy(List<String> groupBy) {
        this.groupBy = groupBy;
        return this;
    }

    public SelectStatementBuilder orderBy(@Nullable org.bithon.server.datasource.query.OrderBy orderBy) {
        this.orderBys = orderBy == null ? null : new OrderByClause[]{new OrderByClause(orderBy.getName(), orderBy.getOrder())};
        return this;
    }

    public SelectStatementBuilder orderBy(List<org.bithon.server.datasource.query.OrderBy> orderBys) {
        this.orderBys = orderBys.stream().map((o) -> new OrderByClause(o.getName(), o.getOrder())).toArray(OrderByClause[]::new);
        return this;
    }

    public SelectStatementBuilder limit(@Nullable org.bithon.server.datasource.query.Limit limit) {
        this.limit = limit;
        return this;
    }

    public SelectStatementBuilder offset(@Nullable HumanReadableDuration offset) {
        this.offset = offset;
        return this;
    }

    public SelectStatementBuilder sqlDialect(ISqlDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
        return this;
    }

    public SelectStatementBuilder querySettings(QuerySettings querySettings) {
        this.querySettings = querySettings;
        return this;
    }

    /**
     * (avgResponse = 5 OR maxResponse = 10) AND s = 's'
     * avgResponse = 5 OR maxResponse = 10
     * avgResponse = 5
     */
    static class FilterSplitter extends AbstractOptimizer {
        private final SelectStatement selectStatement;
        private final List<IExpression> postFilters;

        public FilterSplitter(SelectStatement selectStatement) {
            this.selectStatement = selectStatement;
            this.postFilters = new ArrayList<>();
        }

        @Override
        public IExpression visit(ConditionalExpression expression) {
            IExpression lhs = expression.getLhs();
            if (!(lhs instanceof IdentifierExpression)) {
                return null;
            }
            String identifier = ((IdentifierExpression) lhs).getIdentifier();

            for (Selector selector : selectStatement.getSelectorList().getSelectors()) {
                // If the tag is marked as true, it means this column is the output of an aggregator
                if (selector.getTag() instanceof Boolean && ((boolean) selector.getTag())) {
                    // The TextNode is from Expression, only these filters need to be extracted
                    if (identifier.equals(selector.getOutputName())) {
                        postFilters.add(expression);
                        return null;
                    }
                }
            }

            return expression;
        }
    }

    /**
     * <p>
     * Example:
     * Input:<pre><code>
     *      round(sum(a)/sum(b), 2)
     * </code></pre>
     * Output:<pre><code>
     *      SELECT round(a/b, 2)
     *      FROM (
     *          SELECT sum(a) AS a, sum(b) AS b
     *      )
     * </code></pre>
     * Processing:
     * <pre><code>
     *      sum(a) a, sum(b) b ---> round(a/b, 2)
     * </code></pre>
     * <p>
     * 2. Example result SQL if window function is used for first/last aggregator
     * Input:<pre><code>
     *      (last(queuedTaskCount) + last(activeTaskCount)) / sum(totalTaskCount)
     * </code></pre>
     * <p>
     * Output:<pre><code>
     * SELECT timestamp, (queuedTaskCount + activeTaskCount) / totalTaskCount
     * FROM (
     *      SELECT
     *        "timestamp" AS "_timestamp",
     *        sum("totalTaskCount") AS "totalTaskCount",
     *        queuedTaskCount,
     *        activeTaskCount,
     *      FROM
     *        (
     *          SELECT
     *            UNIX_TIMESTAMP("timestamp")/ 10 * 10 AS "_timestamp",
     *
     *            FIRST_VALUE("queuedTaskCount") OVER (
     *              partition by CAST(toUnixTimestamp("timestamp") / 10 AS Int64) * 10 ORDER BY "timestamp" DESC
     *            ) AS "queuedTaskCount",
     *
     *            "totalTaskCount",
     *
     *            FIRST_VALUE("activeTaskCount") OVER (
     *               partition by CAST(toUnixTimestamp("timestamp") / 10 AS Int64) * 10 ORDER BY "timestamp" DESC
     *            ) AS "activeTaskCount",
     *          FROM
     *            "bithon_thread_pool_metrics"
     *          WHERE
     *            "appName" = 'bithon-server-live'
     *            AND "timestamp" >= fromUnixTimestamp(1666578760)
     *            AND "timestamp" < fromUnixTimestamp(1666589560)
     *        )
     *      GROUP BY
     *        "_timestamp", queuedTaskCount
     *      ORDER BY
     *        "_timestamp"
     * )
     * </code></pre>
     *
     * <p>
     * Processing:<pre><code>
     *     last(activeTaskCount)
     * </code>
     * when visiting the function 'last(activeTaskCount)',
     *  step 1: insert into step to the pipeline at first index
     *  step 2: insert window expression of activeTaskCount to the first step in the pipeline
     *  step 3: insert 'activeTaskCount' to the select list of the 2nd step in the pipeline
     *  step 4: return Identifier expression so that we process it in next phase
     *
     * and when visiting the function expression 'last(queuedTaskCount)'
     *  step 1: insert into step to the pipeline at first index if the first step is not a window expression
     *  repeat step 2 - 4 above since 'last' is a window function
     *
     * and when visiting the function expression 'sum(totalTaskCount)'
     *  step 1: insert into identifier expression totalTaskCount to the first step in the pipeline since the first step is a window function
     *  step 2: insert into aggregation expression sum(totalTaskCount) to the 2nd step in the pipeline
     *  step 3: return Identifier expression so that we process it in next phase
     *
     *  last step: add filter to the first step of the pipeline(push down the pre filter)
     * </pre>
     */
    public SelectStatement build() {
        VariableNameGenerator var = new VariableNameGenerator("_var");

        SelectStatementChain pipeline = new SelectStatementChain();

        Aggregators aggregators = new Aggregators();

        if (this.filter != null) {
            // Apply dialect's transformation on general AST
            this.filter = sqlDialect.transform(this.schema, this.filter, this.querySettings);

            // If filter contains expressions, these expressions should be added to the selector list
            this.filter.accept(new IExpressionInDepthVisitor() {
                @Override
                public boolean visit(IdentifierExpression expression) {
                    IColumn column = schema.getColumnByName(expression.getIdentifier());
                    if (!(column instanceof ExpressionColumn expressionColumn)) {
                        return false;
                    }

                    // Check the existence of the column in the selector list
                    boolean exists = false;
                    for (Selector selector : selectors) {
                        if (selector.getSelectExpression() instanceof ExpressionNode selectExpression) {
                            if (expressionColumn.getExpression().equals(selectExpression.getExpression())) {
                                exists = true;
                                break;
                            }
                        }
                    }

                    if (!exists) {
                        selectors.add(column.toSelector());
                    }
                    return false;
                }
            });
        }

        //
        // Step 1, Determine if there's a post aggregation step
        // For example: round(sum(a)/sum(b), 2) the `round` function will be put in the post aggregation step
        //
        for (Selector selector : this.selectors) {
            IASTNode selectExpression = selector.getSelectExpression();
            if (selectExpression instanceof ExpressionNode expression) {
                IExpression parsedExpression = expression.getParsedExpression();

                if (parsedExpression instanceof FunctionExpression functionExpression) {
                    if (!functionExpression.getFunction().isAggregator()) {
                        pipeline.postAggregation = new SelectStatement();
                        break;
                    }
                } else {
                    pipeline.postAggregation = new SelectStatement();
                    break;
                }
            }
        }

        //
        // Step 2, Replace the aggregation expression
        //
        MacroExpressionSubstitution macroExpressionSubstitution = new MacroExpressionSubstitution(interval);
        for (Selector selector : this.selectors) {
            IASTNode selectExpression = selector.getSelectExpression();
            if (selectExpression instanceof ExpressionNode) {
                IExpression parsedExpression = ((ExpressionNode) selectExpression).getParsedExpression();

                // Replace Macro expressions
                // Don't merge this replacement with the following aggregator replacement
                // Because the macro replacement may contain function call expression which needs to be processed by the following optimizer
                parsedExpression = parsedExpression.accept(macroExpressionSubstitution);

                // Replace all aggregator functions, examples:
                // Case 1: sum(a) ===> a
                // Case 2: round(sum(a)/sum(b), 2) ===> round(a/b, 2), sum(a), sum(b)
                // Case 3: round(sum(a+b), 2) ===> round(a, 2), sum(a+b)
                // Case 4: avg(sum(b)) ===> aggregator is not allowed in another aggregator
                parsedExpression = parsedExpression.accept(new AbstractOptimizer() {
                    @Override
                    public IExpression visit(FunctionExpression functionCallExpression) {
                        if (!functionCallExpression.getFunction().isAggregator()) {
                            return super.visit(functionCallExpression);
                        }

                        IExpression inputArg = functionCallExpression.getArgs().isEmpty() ? null : functionCallExpression.getArgs().get(0);
                        if (inputArg instanceof FunctionExpression && ((FunctionExpression) inputArg).getFunction().isAggregator()) {
                            throw new InvalidExpressionException("Aggregator [%s] is not allowed in another aggregator [%s].", inputArg.serializeToText(), functionCallExpression.getName());
                        }

                        String output;
                        IDataType dataType;
                        if (pipeline.postAggregation == null) {
                            // If there's no post-aggregation, the output should be the same as the input
                            output = selector.getOutputName();
                            dataType = functionCallExpression.getDataType();
                        } else {
                            if (functionCallExpression.getFunction() instanceof QueryStageFunctions.Cardinality) {
                                // the default converted expression for  cardinality(a) is count(distinct a) as a,
                                // where a usually is a string column which might cause the generated SQL confusion for debugging
                                // So we generate a new name for the output
                                output = "cardinality" + var.next();
                                dataType = IDataType.LONG;
                            } else {
                                if (inputArg instanceof IdentifierExpression) {
                                    output = ((IdentifierExpression) inputArg).getIdentifier();
                                    dataType = functionCallExpression.getDataType();
                                } else {
                                    // This might be the form: sum(a+b)
                                    // Is there such input: sum(round(a/b,2))
                                    output = var.next();
                                    dataType = IDataType.LONG;
                                }
                            }
                        }
                        aggregators.add(functionCallExpression, output);

                        if (pipeline.windowAggregation == null && sqlDialect.useWindowFunctionAsAggregator(functionCallExpression.getName())) {
                            pipeline.windowAggregation = new SelectStatement();
                        }

                        // Replace the aggregator in original expression to reference the output
                        return new IdentifierExpression(output, dataType);
                    }
                });

                ((ExpressionNode) selectExpression).setParsedExpression(parsedExpression);
            }
        }

        Set<String> nonWindowAggregators = new LinkedHashSet<>();

        for (int i = 0, aggregatorsSize = aggregators.size(); i < aggregatorsSize; i++) {
            Aggregator aggregator = aggregators.get(i);
            FunctionExpression aggregateFunction = aggregator.aggregateFunction;

            if (sqlDialect.useWindowFunctionAsAggregator(aggregateFunction.getName())) { // If this function is a window function
                if (!(aggregateFunction.getArgs().get(0) instanceof IdentifierExpression identifierExpression)) {
                    // For simply, currently only IdentifierExpression is allowed in window aggregators
                    throw new InvalidExpressionException("Only field is allowed in aggregator [%s]", aggregateFunction.getName());
                }

                String col = identifierExpression.getIdentifier();
                IExpression windowFunctionExpression = sqlDialect.firstWindowFunction(col, interval.getTotalSeconds());
                pipeline.windowAggregation.getSelectorList().add(new ExpressionNode(windowFunctionExpression), aggregator.output, IDataType.DOUBLE);
                pipeline.aggregation.getSelectorList()
                                    .add(new Column(aggregator.output), identifierExpression.getDataType())
                                    .setTag(true); // mark this column as output of an aggregator
            } else { // this aggregator function is NOT a window function
                pipeline.aggregation.getSelectorList()
                                    .add(new ExpressionNode(aggregator.aggregateFunction), aggregator.output, aggregator.aggregateFunction.getDataType())
                                    .setTag(true); // mark this column as output of an aggregator

                if (pipeline.windowAggregation != null) {

                    // Push all columns referenced in the aggregator to the window aggregation step
                    nonWindowAggregators.addAll(
                        IdentifierExtractor.extractIdentifiers(schema, aggregateFunction)
                    );
                }
            }
        }

        // All columns in the aggregation step must appear in the window aggregation step
        if (pipeline.windowAggregation != null) {
            for (String column : nonWindowAggregators) {
                pipeline.windowAggregation.getSelectorList().add(new Column(column), IDataType.DOUBLE);
            }
        }

        // Add post aggregation column expressions
        if (pipeline.postAggregation != null) {
            for (Selector selector : this.selectors) {
                IASTNode selectExpression = selector.getSelectExpression();
                if (selectExpression instanceof ExpressionNode) {
                    IExpression parsedExpression = ((ExpressionNode) selectExpression).getParsedExpression();

                    if (parsedExpression instanceof IdentifierExpression identifierExpression) {
                        // Try to eliminate Alias expression
                        String identifier = identifierExpression.getIdentifier();
                        pipeline.postAggregation.getSelectorList()
                                                .add(new Column(identifier), identifier.equals(selector.getOutput().getName()) ? null : selector.getOutput(), identifierExpression.getDataType())
                                                .setTag(true);
                    } else {
                        pipeline.postAggregation.getSelectorList()
                                                .add(new ExpressionNode(parsedExpression), selector.getOutput(), parsedExpression.getDataType())
                                                .setTag(true);
                    }
                }
            }
        }

        //
        // Add a sliding window aggregation step if WINDOW and INTERVAL are different
        //
        if (hasSlidingWindowAggregation()) {
            addSlidingWindowAggregationStep(aggregators, pipeline);
        }

        //
        // Chain the pipelines together
        //
        pipeline.chain(this.sqlDialect);
        pipeline.innermost.getFrom().setExpression(new TableIdentifier(schema.getDataStoreSpec().getStore()));

        // Build GroupBy first, because we might need to move some filters to the group-by as HAVING
        buildGroupBy(pipeline);
        buildWhere(pipeline);
        buildOrderBy(pipeline);
        buildLimit(pipeline);

        //
        // Apply DBMS specific transformation
        //
        SelectStatement selectStatement = pipeline.outermost;
        do {
            for (Selector selector : selectStatement.getSelectorList().getSelectors()) {
                if (selector.getSelectExpression() instanceof ExpressionNode expression) {
                    IExpression parsedExpression = expression.getParsedExpression();
                    parsedExpression = sqlDialect.transform(this.schema, parsedExpression, querySettings);
                    expression.setParsedExpression(parsedExpression);
                }
            }

            selectStatement = selectStatement.getFrom().getExpression() instanceof SelectStatement nestSelect ? nestSelect : null;
        } while (selectStatement != null);

        // returns the outermost pipeline
        return pipeline.outermost;
    }

    /**
     * Add a sliding window aggregation step to the pipeline, which produces SQL as follows:
     * <p>
     * <pre><code>
     * SELECT "_timestamp",
     *   sum("responseTime") OVER (ORDER BY "_timestamp" ASC RANGE BETWEEN 300 PRECEDING AND 0 FOLLOWING) AS "responseTime",
     *  sum("totalCount") OVER (ORDER BY "_timestamp" ASC RANGE BETWEEN 300 PRECEDING AND 0 FOLLOWING) AS "totalCount"
     * FROM
     * </code></pre>
     */
    private void addSlidingWindowAggregationStep(Aggregators aggregators, SelectStatementChain pipeline) {
        SelectStatement slidingWindowAggregation = new SelectStatement();

        IExpression[] partitionByExpression = null;
        if (CollectionUtils.isNotEmpty(this.groupBy)) {
            partitionByExpression = this.groupBy.stream()
                                                .map(IdentifierExpression::new)
                                                .toList()
                                                .toArray(new IExpression[0]);
        }

        // Add Aggregated fields to the sliding window statement as WINDOW FUNCTION
        for (Aggregator aggregator : aggregators.aggregators) {
            WindowFunctionExpression windowFunctionExpression = WindowFunctionExpression.builder()
                                                                                        .name(aggregator.aggregateFunction.getName())
                                                                                        .args(new ArrayList<>(Collections.singletonList(IdentifierExpression.of(aggregator.output))))
                                                                                        .partitionBy(partitionByExpression)
                                                                                        .orderBy(new OrderByElement(IdentifierExpression.of(TimestampSpec.COLUMN_ALIAS)))
                                                                                        .frame(new WindowFunctionFrame(LiteralExpression.ofLong(interval.getWindow().getDuration().getSeconds()), LiteralExpression.ofLong(0)))
                                                                                        .build();

            slidingWindowAggregation.getSelectorList()
                                    .add(new ExpressionNode(windowFunctionExpression), aggregator.output, aggregator.aggregateFunction.getDataType());
        }

        // Apply a timestamp filter to the sliding window aggregation step
        // This is because we extend the sliding window to make sure the first record is correct
        //
        // The sliding window aggregation is performed on aggregated step which already on floored timestamp,
        // So we need to floor the start timestamp to make sure time range is correct.
        // Since the end timestamp is EXCLUSIVE, there's no need to floor it.
        TimeSpan startWindow = interval.getStartTime().floor(interval.getStep());
        TimeSpan endWindow = interval.getEndTime();

        // The inner query of thiw sliding window aggregation step already offset the timestamp,
        // So we need to add the offset to the start and end timestamp to make sure we're filtering the correct time range
        if (this.offset != null) {
            // The offset is negative, so we need to add the offset
            startWindow = startWindow.after(-this.offset.getDuration().getSeconds(), TimeUnit.SECONDS);
            endWindow = endWindow.after(-this.offset.getDuration().getSeconds(), TimeUnit.SECONDS);
        }

        slidingWindowAggregation.getWhere().and(toTimestampFilter(new IdentifierExpression(TimestampSpec.COLUMN_ALIAS),
                                                                  startWindow,
                                                                  endWindow,
                                                                  false));

        pipeline.slidingWindowAggregation = slidingWindowAggregation;
    }

    private void buildLimit(SelectStatementChain pipeline) {
        if (limit == null) {
            return;
        }
        Preconditions.checkNotNull(this.orderBys, "Limit must be used with order by clause");

        pipeline.outermost.setLimit(new LimitClause(limit.getLimit(), limit.getOffset()));
    }

    private void buildOrderBy(SelectStatementChain pipeline) {
        if (orderBys == null) {
            return;
        }

        pipeline.outermost.setOrderBy(this.orderBys);
    }

    private void buildWhere(SelectStatementChain pipeline) {
        // Extend the time range for the sliding window
        // so that the sliding window calculation of the first record is correct
        TimeSpan start = hasSlidingWindowAggregation() ? interval.getStartTime().before(interval.getWindow().getDuration()) : interval.getStartTime();
        pipeline.innermost.getWhere()
                          .and(toTimestampFilter(this.interval.getTimestampColumn(), start, interval.getEndTime(), true));

        if (filter == null) {
            return;
        }

        // Separate the filter to pre-filter and post-filter
        FilterSplitter splitter = new FilterSplitter(pipeline.outermost);
        filter = filter.accept(splitter);
        if (filter != null) {
            // The user filters might filter on a column that is being aggregated, to make the query work,
            // We think this filter is NOT a post filter on the result of aggregation but on the original column.
            // So we need to transform the filter first to make it qualified
            pipeline.innermost.getWhere().and(filter.accept(new QualifiedIdentifierTransformer(schema)));
        }

        if (splitter.postFilters.isEmpty()) {
            return;
        }

        IExpression postFilter = splitter.postFilters.size() > 1 ? new LogicalExpression.AND(splitter.postFilters) : splitter.postFilters.get(0);
        if (!pipeline.outermost.getGroupBy().getFields().isEmpty()) {
            pipeline.outermost.setHaving(new HavingClause());
            pipeline.outermost.getHaving().addExpression(Expression2Sql.from((String) null, sqlDialect, postFilter));
        } else {
            pipeline.outermost.getWhere().and(postFilter);
        }
    }

    private void buildGroupBy(SelectStatementChain pipeline) {
        pipeline.aggregation.getGroupBy().addFields(groupBy);

        // All window aggregation output must appear in the group-by clause
        if (pipeline.windowAggregation != null) {
            for (Selector column : pipeline.windowAggregation.getSelectorList().getSelectors()) {
                if (!(column.getSelectExpression() instanceof Column)) {
                    pipeline.aggregation.getGroupBy().addField(column.getOutputName());
                }
            }
        }

        // All input group-by fields must appear in the select list of all steps
        // Since we always insert the column to the first place of the select list,
        // we iterate the list in reversed order
        for (int i = this.groupBy.size() - 1; i >= 0; i--) {
            String groupBy = this.groupBy.get(i);

            if (pipeline.windowAggregation != null) {
                pipeline.windowAggregation.getSelectorList().insert(new Column(groupBy), IDataType.STRING);
            }

            pipeline.aggregation.getSelectorList().insert(new Column(groupBy), IDataType.STRING);

            if (pipeline.slidingWindowAggregation != null) {
                pipeline.slidingWindowAggregation.getSelectorList().insert(new Column(groupBy), IDataType.STRING);
            }

            if (pipeline.postAggregation != null) {
                pipeline.postAggregation.getSelectorList().insert(new Column(groupBy), IDataType.STRING);
            }
        }

        // We need to group the values in a time bucket
        // See the test: testWindowFunction_TimeSeries to know more
        if (this.interval.getStep() != null) {
            String timestamp = this.sqlDialect.timeFloorExpression(this.interval.getTimestampColumn(), this.interval.getStep().getSeconds());
            if (this.offset != null) {
                // When offset is provided, the timestamp in result set is still produced as if no offset
                timestamp = StringUtils.format("%s + %d", timestamp,
                                               // The offset is negative, so we need to add the offset
                                               -this.offset.getDuration().getSeconds());
            }
            TextNode expr = new TextNode(timestamp);

            // The timestamp calculation is pushed down to the window aggregation step if needed
            SelectStatement aggregationStep = pipeline.windowAggregation == null ? pipeline.aggregation : pipeline.windowAggregation;
            aggregationStep.getSelectorList().insert(expr, TimestampSpec.COLUMN_ALIAS, IDataType.DATETIME_MILLI);

            // Always add the timestamp to the group-by clause of the aggregation step
            pipeline.aggregation.getGroupBy().addField(TimestampSpec.COLUMN_ALIAS);
            if (aggregationStep != pipeline.aggregation) {
                // Add timestamp to the SELECT list of the aggregation step
                pipeline.aggregation.getSelectorList().insert(TimestampSpec.COLUMN_ALIAS, IDataType.DATETIME_MILLI);
            }

            if (pipeline.slidingWindowAggregation != null) {
                pipeline.slidingWindowAggregation.getSelectorList().insert(TimestampSpec.COLUMN_ALIAS, IDataType.DATETIME_MILLI);
            }

            // Add timestamp to the SELECT list of the final step
            if (pipeline.postAggregation != null) {
                pipeline.postAggregation.getSelectorList().insert(TimestampSpec.COLUMN_ALIAS, IDataType.DATETIME_MILLI);
            }
        }
    }

    /**
     * @param start INCLUSIVE
     * @param end   EXCLUSIVE
     */
    private IExpression[] toTimestampFilter(IExpression timestampColumn, TimeSpan start, TimeSpan end, boolean useTimestampText) {
        if (this.offset != null) {
            // The offset is negative, use '-' to turn into positive
            start = start.before(-this.offset.getDuration().getSeconds(), TimeUnit.SECONDS);
            end = end.before(-this.offset.getDuration().getSeconds(), TimeUnit.SECONDS);
        }

        return new IExpression[]{
            new ComparisonExpression.GTE(timestampColumn, useTimestampText ? sqlDialect.toISO8601TimestampExpression(start) : LiteralExpression.of(start.getMilliseconds() / 1000)),
            new ComparisonExpression.LT(timestampColumn, useTimestampText ? sqlDialect.toISO8601TimestampExpression(end) : LiteralExpression.of(end.getMilliseconds() / 1000)),
            };
    }
}
