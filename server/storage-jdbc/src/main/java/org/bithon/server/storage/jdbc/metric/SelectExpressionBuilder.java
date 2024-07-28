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

import jakarta.annotation.Nullable;
import lombok.Getter;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.MacroExpression;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.component.commons.expression.optimzer.ExpressionOptimizer;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.column.aggregatable.IAggregatableColumn;
import org.bithon.server.storage.datasource.query.ast.Column;
import org.bithon.server.storage.datasource.query.ast.ColumnAlias;
import org.bithon.server.storage.datasource.query.ast.Expression;
import org.bithon.server.storage.datasource.query.ast.From;
import org.bithon.server.storage.datasource.query.ast.Function;
import org.bithon.server.storage.datasource.query.ast.GroupBy;
import org.bithon.server.storage.datasource.query.ast.IASTNode;
import org.bithon.server.storage.datasource.query.ast.Limit;
import org.bithon.server.storage.datasource.query.ast.OrderBy;
import org.bithon.server.storage.datasource.query.ast.QueryExpression;
import org.bithon.server.storage.datasource.query.ast.SelectColumn;
import org.bithon.server.storage.datasource.query.ast.StringNode;
import org.bithon.server.storage.datasource.query.ast.Table;
import org.bithon.server.storage.datasource.query.ast.Where;
import org.bithon.server.storage.datasource.query.parser.FieldExpressionVisitorAdaptor2;
import org.bithon.server.storage.jdbc.common.dialect.Expression2Sql;
import org.bithon.server.storage.jdbc.common.dialect.ISqlDialect;
import org.bithon.server.storage.metrics.Interval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/10/30 11:52
 */
public class SelectExpressionBuilder {

    public static final String TIMESTAMP_ALIAS_NAME = "_timestamp";

    private ISchema schema;

    private List<SelectColumn> selectColumns;

    private IExpression filter;
    private Interval interval;

    private List<String> groupBy = Collections.emptyList();

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

    public SelectExpressionBuilder fields(List<SelectColumn> selectColumns) {
        this.selectColumns = selectColumns;
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

    static class Expression2SqlSerializer extends Expression2Sql {
        protected final Map<String, Object> variables;

        Expression2SqlSerializer(ISqlDialect sqlDialect, Map<String, Object> variables) {
            super(null, sqlDialect);
            this.variables = variables;
        }

        @Override
        public boolean visit(MacroExpression expression) {
            Object variableValue = variables.get(expression.getMacro());
            if (variableValue == null) {
                throw new RuntimeException(StringUtils.format("variable (%s) not provided in context",
                                                              expression.getMacro()));
            }
            sb.append(variableValue);

            return false;
        }
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
            if (!(columnSpec instanceof IAggregatableColumn metricSpec)) {
                throw new RuntimeException(StringUtils.format("field [%s] is not a metric", columnSpec.getName()));
            }

            if (aggregatedColumn.contains(metricSpec.getName())) {
                return;
            }

            if (sqlExpressionFormatter.useWindowFunctionAsAggregator(metricSpec.getAggregateFunctionExpression().getName())) {
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
                    boolean useWindowFunctionAsAggregator = sqlDialect.useWindowFunctionAsAggregator(metricSpec.getAggregateFunctionExpression().getName());

                    // Case 2. Some DB does not allow same aggregation expressions, we use the existing expression
                    boolean hasSameExpression = !sqlDialect.allowSameAggregatorExpression() && aggregatedFields.contains(metricSpec.getName());

                    if (useWindowFunctionAsAggregator || hasSameExpression) {
                        sb.append(sqlDialect.quoteIdentifier(metricSpec.getName()));
                    } else {
                        // generate a aggregation expression
                        sb.append(sqlDialect.quoteIdentifier(field));
                        //sb.append(sqlGenerator4SimpleAggregationFunction.generate(metricSpec.getAggregateExpression()));
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

            return new StringNode(serializer.serialize(expression.getParsedExpression(schema)));
        }
    }

    static class IdentifierExtractor extends FieldExpressionVisitorAdaptor2 {
        private final ISchema schema;

        @Getter
        private final Set<String> identifiers = new LinkedHashSet<>();

        IdentifierExtractor(ISchema schema) {
            this.schema = schema;
        }

        @Override
        public void visitField(IColumn columnSpec) {
            identifiers.add(columnSpec.getName());
        }

        @Override
        protected ISchema getSchema() {
            return schema;
        }

        public static Set<String> extractIdentifiers(ISchema schema, IExpression expression) {
            IdentifierExtractor extractor = new IdentifierExtractor(schema);
            expression.accept(extractor);
            return extractor.getIdentifiers();
        }
    }

    static class Variable {
        private int index;
        private final String prefix;

        Variable(String prefix) {
            this.prefix = prefix;
        }

        public String next() {
            return prefix + index++;
        }
    }

    static class Aggregator {
        private final FunctionExpression expression;
        private final String output;
        private final boolean isSimpleAggregation;

        Aggregator(FunctionExpression expression, String output) {
            this.expression = expression;
            this.output = output;
            this.isSimpleAggregation = expression.getArgs().get(0) instanceof IdentifierExpression;
        }
    }

    static class Aggregators {
        private final List<Aggregator> aggregators = new ArrayList<>();

        /**
         * Add an aggregation.
         * This also checks there's no same aggregation performed on the same column
         */
        public void add(FunctionExpression functionCallExpression, String output) {
            if (!contains(functionCallExpression)) {
                aggregators.add(new Aggregator(functionCallExpression, output));
            }
        }

        private boolean contains(FunctionExpression functionExpression) {
            if (!(functionExpression.getArgs().get(0) instanceof IdentifierExpression)) {
                return false;
            }

            return aggregators.stream()
                              .anyMatch(aggregator -> {
                                  if (!aggregator.isSimpleAggregation) {
                                      return false;
                                  }

                                  if (!aggregator.expression.getName().equals(functionExpression.getName())) {
                                      return false;
                                  }

                                  String col = ((IdentifierExpression) aggregator.expression.getArgs().get(0)).getIdentifier();
                                  return col.equals(((IdentifierExpression) functionExpression.getArgs().get(0)).getIdentifier());
                              });
        }

        public int size() {
            return aggregators.size();
        }

        public Aggregator get(int index) {
            return aggregators.get(index);
        }
    }

    public static class Pipeline {
        private QueryExpression windowAggregation;
        private QueryExpression aggregation = new QueryExpression();
        private QueryExpression postAggregation;
        private boolean hasAggregation = true;

        private QueryExpression outermost;
        private QueryExpression innermost;

        public void chain() {
            List<QueryExpression> pipelines = new ArrayList<>();
            if (windowAggregation != null) {
                pipelines.add(windowAggregation);
            }
            if (hasAggregation) {
                pipelines.add(aggregation);
            }
            if (postAggregation != null) {
                pipelines.add(postAggregation);
            }

            for (int i = 1; i < pipelines.size(); i++) {
                From from = pipelines.get(i).getFrom();
                from.setExpression(pipelines.get(i - 1));

                // For MySQL, the sub-query must have an alias
                from.setAlias(new ColumnAlias("tbl" + i));
            }

            this.outermost = pipelines.get(pipelines.size() - 1);
            this.innermost = pipelines.get(0);
        }

        public QueryExpression getAggregationStep() {
            return hasAggregation ? aggregation : windowAggregation;
        }
    }

    public QueryExpression buildPipeline() {
        Variable var = new Variable("a");

        Map<String, Object> macros = Map.of("interval", interval.getStep() == null ? interval.getTotalLength() : interval.getStep(),
                                            "instanceCount", StringUtils.format("count(distinct %s)", sqlDialect.quoteIdentifier("instanceName")));

        Pipeline pipeline = new Pipeline();

        Aggregators aggregators = new Aggregators();

        // Step 1
        // Find and replace the aggregation expression
        for (SelectColumn selectColumn : this.selectColumns) {
            IASTNode selectExpression = selectColumn.getSelectExpression();
            if (selectExpression instanceof Expression) {
                IExpression parsedExpression = ((Expression) selectExpression).getParsedExpression(this.schema);

                // Replace all aggregator functions
                // Case 1: sum(a) ===> a
                // Case 2: round(sum(a)/sum(b), 2) ===> round(a/b, 2), sum(a), sum(b)
                // Case 3: round(sum(a+b), 2) ===> round(a, 2), sum(a+b)
                // Case 4: avg(sum(b)) ===> aggregator is not allowed in another aggregator
                parsedExpression = parsedExpression.accept(new ExpressionOptimizer.AbstractOptimizer() {
                    @Override
                    public IExpression visit(FunctionExpression functionCallExpression) {
                        if (!functionCallExpression.getFunction().isAggregator()) {
                            return super.visit(functionCallExpression);
                        }

                        IExpression inputArg = functionCallExpression.getArgs().get(0);
                        if (inputArg instanceof FunctionExpression && ((FunctionExpression) inputArg).getFunction().isAggregator()) {
                            throw new InvalidExpressionException("Aggregator [%s] is not allowed in another aggregator [%s].", inputArg.serializeToText(), functionCallExpression.getName());
                        }

                        String output;
                        if (inputArg instanceof IdentifierExpression) {
                            output = ((IdentifierExpression) inputArg).getIdentifier();
                        } else {
                            // This might be the form: sum(a+b)
                            // Is there such input: sum(round(a/b,2))
                            output = var.next();
                        }

                        aggregators.add(functionCallExpression, output);

                        // Replace the aggregator in original expression to reference the output
                        return new IdentifierExpression(output);
                    }
                });

                ((Expression) selectExpression).setParsedExpression(parsedExpression);

                // After pushing down aggregation,
                // if the expression still contains further calculation on the aggregation result,
                // we need to add a step to take the calculation
                if (!(parsedExpression instanceof IdentifierExpression)) {
                    pipeline.postAggregation = new QueryExpression();
                }
            }
        }

        Set<String> nonWindowAggregators = new LinkedHashSet<>();

        for (int i = 0, aggregatorsSize = aggregators.size(); i < aggregatorsSize; i++) {
            Aggregator aggregator = aggregators.get(i);
            FunctionExpression functionCall = aggregator.expression;

            if (sqlDialect.useWindowFunctionAsAggregator(functionCall.getName())) { // If this function is a window function
                if (pipeline.windowAggregation == null) {
                    pipeline.windowAggregation = new QueryExpression();
                }

                if (!(functionCall.getArgs().get(0) instanceof IdentifierExpression identifierExpression)) {
                    // For simply, currently only IdentifierExpression is allowed in window aggregators
                    throw new InvalidExpressionException("Only field is allowed in aggregator [%s]", functionCall.getName());
                }

                String col = identifierExpression.getIdentifier();
                String output = col;
                String windowAggregator = sqlDialect.firstAggregator(col,
                                                                     interval.getTotalLength());
                pipeline.windowAggregation.getSelectColumnList().add(new StringNode(windowAggregator), output);
                pipeline.aggregation.getSelectColumnList().add(new Column(output));
            } else { // this aggregator function is NOT a window function
                pipeline.hasAggregation = true;
                pipeline.aggregation.getSelectColumnList().add(new Function(aggregator.expression, ""), aggregator.output);

                if (pipeline.windowAggregation != null) {

                    // Push all fields referenced in the aggregator to the window aggregation step
                    nonWindowAggregators.addAll(
                        IdentifierExtractor.extractIdentifiers(schema, functionCall)
                    );
                }
            }
        }

        // All columns in the aggregation step must appear in the window aggregation step
        if (pipeline.windowAggregation != null) {
            for (String column : nonWindowAggregators) {
                pipeline.windowAggregation.getSelectColumnList().add(new Column(column));
            }
        }

        // Add post aggregation column expressions
        if (pipeline.postAggregation != null) {
            for (SelectColumn selectColumn : this.selectColumns) {
                IASTNode selectExpression = selectColumn.getSelectExpression();
                if (selectExpression instanceof Expression) {
                    IExpression parsedExpression = ((Expression) selectExpression).getParsedExpression(this.schema);

                    pipeline.postAggregation.getSelectColumnList().add(new StringNode(new Expression2SqlSerializer(this.sqlDialect, macros).serialize(parsedExpression)), selectColumn.getOutput());
                }
            }
        }

        //
        // Chain the pipelines together
        //
        pipeline.chain();
        QueryExpression outermost = pipeline.outermost;
        QueryExpression innermost = pipeline.innermost;
        innermost.getFrom().setExpression(new Table(schema.getDataStoreSpec().getStore()));

        //
        // Set Where expression to the innermost
        //
        IExpression timestampExpression = this.interval.getTimestampColumn();
        Where where = new Where();
        where.addExpression(StringUtils.format("%s >= %s", Expression2Sql.from((String) null, sqlDialect, timestampExpression), sqlDialect.formatTimestamp(interval.getStartTime())));
        where.addExpression(StringUtils.format("%s < %s", Expression2Sql.from((String) null, sqlDialect, timestampExpression), sqlDialect.formatTimestamp(interval.getEndTime())));
        if (filter != null) {
            where.addExpression(Expression2Sql.from(schema, sqlDialect, filter));
        }
        innermost.setWhere(where);

        //
        // Set GroupBy expression to the aggregation
        //
        if (pipeline.hasAggregation) {
            pipeline.aggregation.getGroupBy().addFields(groupBy);

            // All window aggregation output must appear in the group-by clause
            if (pipeline.windowAggregation != null) {
                for (SelectColumn column : pipeline.windowAggregation.getSelectColumnList().getColumns()) {
                    pipeline.aggregation.getGroupBy().addField(column.getOutputName());
                }
            }

            // All input group-by fields must appear in the select list of all steps
            // Since we always insert the column to the first place of the select list,
            // we iterate the list in reversed order
            for (int i = this.groupBy.size() - 1; i >= 0; i--) {
                String groupBy = this.groupBy.get(i);

                if (pipeline.windowAggregation != null) {
                    pipeline.windowAggregation.getSelectColumnList().insert(new Column(groupBy));
                }

                pipeline.aggregation.getSelectColumnList().insert(new Column(groupBy));

                if (pipeline.postAggregation != null) {
                    pipeline.postAggregation.getSelectColumnList().insert(new Column(groupBy));
                }
            }
        }

        // We need to group the values in a time bucket
        // See the test: testWindowFunction_TimeSeries to know more
        if (this.interval.getStep() != null) {
            StringNode expr = new StringNode(this.sqlDialect.timeFloorExpression(timestampExpression, this.interval.getStep().getSeconds()));

            // The timestamp calculation is pushed down to the window aggregation step if needed
            QueryExpression aggregationStep = pipeline.windowAggregation == null ? pipeline.aggregation : pipeline.windowAggregation;
            aggregationStep.getSelectColumnList().insert(expr, TIMESTAMP_ALIAS_NAME);

            // Always add the timestamp to the group-by clause of the aggregation step
            pipeline.aggregation.getGroupBy().addField(TIMESTAMP_ALIAS_NAME);
            if (aggregationStep != pipeline.aggregation) {
                // Add timestamp to the SELECT list of the aggregation step
                pipeline.aggregation.getSelectColumnList().insert(TIMESTAMP_ALIAS_NAME);
            }

            // Add timestamp to the SELECT list of the final step
            if (pipeline.postAggregation != null) {
                pipeline.postAggregation.getSelectColumnList().insert(TIMESTAMP_ALIAS_NAME);
            }
        }

        //
        // Set OrderBy/Limit expression to the outermost
        //
        if (orderBy != null) {
            outermost.setOrderBy(new OrderBy(orderBy.getName(), orderBy.getOrder()));
        }
        if (limit != null) {
            outermost.setLimit(new Limit(limit.getLimit(), limit.getOffset()));
        }

        // returns the outermost pipeline
        return pipeline.outermost;
    }

    /**
     * TODO: Change the pipelines to represent the sub-queries
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
     * </code></pre>
     */
    public QueryExpression build() {
        String sqlTableName = schema.getDataStoreSpec().getStore();

        //
        // Turn some metrics (those use window functions for aggregation) in expression into pre-aggregator first
        //
        Set<String> aggregatedFields = this.selectColumns.stream()
                                                         .filter((f) -> f.getSelectExpression() instanceof Function)
                                                         .map(selectColumn -> ((Function) selectColumn.getSelectExpression()).getField())
                                                         .collect(Collectors.toSet());

        QueryExpression queryExpression = new QueryExpression();
        queryExpression.setGroupBy(new GroupBy());
        QueryExpression subQueryExpression = new QueryExpression();

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
                                                                                      Map.of("interval",
                                                                                             interval.getStep() == null ? interval.getTotalLength() : interval.getStep(),
                                                                                             "instanceCount",
                                                                                             StringUtils.format("count(distinct %s)", sqlDialect.quoteIdentifier("instanceName"))));

        for (SelectColumn selectColumn : this.selectColumns) {
            IASTNode columnExpression = selectColumn.getSelectExpression();
            if (columnExpression instanceof Function function) {

                // if window function is contained, the final SQL has a sub-query
                if (sqlDialect.useWindowFunctionAsAggregator(function.getExpression().getName())) {
                    subQueryExpression.getSelectColumnList().add(new StringNode(generator.generate(function.getExpression())), selectColumn.getOutput());

                    // this window fields should be in the group-by clause and select clause,
                    // see the Javadoc above
                    // Use name in the groupBy expression because we have alias for the corresponding field in sub-query expression
                    queryExpression.getGroupBy().addField(selectColumn.getOutput().getName());
                    queryExpression.getSelectColumnList().add(selectColumn.getOutput().getName());

                    hasSubSelect = true;
                } else {
                    queryExpression.getSelectColumnList().add(new StringNode(generator.generate(function.getExpression())), selectColumn.getOutput());

                    String underlyingFieldName = function.getField();

                    // This metric should also be in the sub-query, see the example in the Javadoc above
                    subQueryExpression.getSelectColumnList().add(underlyingFieldName);
                }
            } else if (columnExpression instanceof Expression) {
                queryExpression.getSelectColumnList().add(sqlGenerator4Expression.visit((Expression) columnExpression),
                                                          selectColumn.getOutput());
            } else if (columnExpression instanceof Column) {
                queryExpression.getSelectColumnList().add(columnExpression, selectColumn.getOutput());
            } else {
                throw new RuntimeException(StringUtils.format("Invalid field[%s] with type[%s]", selectColumn.toString(), selectColumn.getClass().getName()));
            }
        }

        // Make sure all referenced metrics in field expression are in the sub-query
        FieldExpressionAnalyzer fieldExpressionAnalyzer = new FieldExpressionAnalyzer(this.schema, aggregatedFields, this.sqlDialect);
        this.selectColumns.stream()
                          .filter((f) -> f.getSelectExpression() instanceof Expression)
                          .forEach((f) -> ((Expression) f.getSelectExpression()).getParsedExpression(schema).accept(fieldExpressionAnalyzer));
        for (String metric : fieldExpressionAnalyzer.getMetrics()) {
            subQueryExpression.getSelectColumnList().add(metric);
        }
        for (IAggregatableColumn aggregator : fieldExpressionAnalyzer.getWindowFunctionAggregators()) {
            subQueryExpression.getSelectColumnList()
                              .add(new StringNode(generator.generate(aggregator.getAggregateFunctionExpression())), aggregator.getName());

            // this window fields should be in the group-by clause and select clause,
            // see the Javadoc above
            // Use name in the groupBy expression because we have alias for the corresponding field in sub-query expression
            queryExpression.getGroupBy().addField(aggregator.getName());
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
        subQueryExpression.getSelectColumnList().addAll(groupBy);
        queryExpression.getGroupBy().addFields(groupBy);

        // Make sure all fields in the groupBy are in the field list
        if (!groupBy.isEmpty()) {
            Set<String> existingFields = queryExpression.getSelectColumnList().getColumnNames(Collectors.toSet());

            for (String name : groupBy) {
                if (existingFields.add(name)) {
                    IASTNode column = new Column(name);

                    queryExpression.getSelectColumnList().add(column);
                    subQueryExpression.getSelectColumnList().add(column);
                }
            }
        }

        //
        // build OrderBy/Limit expression
        //
        if (orderBy != null) {
            queryExpression.setOrderBy(new OrderBy(orderBy.getName(), orderBy.getOrder()));
        }
        if (limit != null) {
            queryExpression.setLimit(new Limit(limit.getLimit(), limit.getOffset()));
        }

        //
        // Link query and subQuery together
        //
        if (hasSubSelect) {
            subQueryExpression.getFrom().setExpression(new Table(sqlTableName));
            subQueryExpression.setWhere(where);
            queryExpression.getFrom().setExpression(subQueryExpression);

            // For MySQL, the sub-query must have an alias
            queryExpression.getFrom().setAlias(new ColumnAlias("nest"));
        } else {
            queryExpression.getFrom().setExpression(new Table(sqlTableName));
            queryExpression.setWhere(where);
        }
        return queryExpression;
    }
}
