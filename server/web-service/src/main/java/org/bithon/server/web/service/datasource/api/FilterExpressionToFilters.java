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

package org.bithon.server.web.service.datasource.api;

import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.matcher.BetweenMatcher;
import org.bithon.server.commons.matcher.EqualMatcher;
import org.bithon.server.commons.matcher.GreaterThanMatcher;
import org.bithon.server.commons.matcher.GreaterThanOrEqualMatcher;
import org.bithon.server.commons.matcher.IMatcherVisitor;
import org.bithon.server.commons.matcher.InMatcher;
import org.bithon.server.commons.matcher.LessThanMatcher;
import org.bithon.server.commons.matcher.LessThanOrEqualMatcher;
import org.bithon.server.commons.matcher.NotEqualMatcher;
import org.bithon.server.commons.matcher.NotMatcher;
import org.bithon.server.commons.matcher.StringAntPathMatcher;
import org.bithon.server.commons.matcher.StringContainsMatcher;
import org.bithon.server.commons.matcher.StringEndWithMatcher;
import org.bithon.server.commons.matcher.StringIContainsMatcher;
import org.bithon.server.commons.matcher.StringLikeMatcher;
import org.bithon.server.commons.matcher.StringRegexMatcher;
import org.bithon.server.commons.matcher.StringStartsWithMatcher;
import org.bithon.server.storage.common.expression.ExpressionASTBuilder;
import org.bithon.server.storage.common.expression.InvalidExpressionException;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.filter.IColumnFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/5/6 23:49
 */
public class FilterExpressionToFilters {

    public static IExpression toExpression(DataSourceSchema schema, String filterExpression, List<IColumnFilter> otherFilters) {
        if (StringUtils.isEmpty(filterExpression)) {
            return CollectionUtils.isEmpty(otherFilters) ? null : toExpression(schema, otherFilters);
        }

        IExpression expression = ExpressionASTBuilder.build(filterExpression);
        expression.accept(new IdentifierVerifier(schema));

        if (CollectionUtils.isNotEmpty(otherFilters)) {
            if (expression instanceof LogicalExpression.AND) {
                ((LogicalExpression.AND) expression).getOperands().add(toExpression(schema, otherFilters));
                return expression;
            } else {
                return new LogicalExpression.AND(expression, toExpression(schema, otherFilters));
            }
        } else {
            return expression;
        }
    }

    private static IExpression toExpression(DataSourceSchema schema, List<IColumnFilter> filters) {
        IdentifierVerifier identifierVerifier = new IdentifierVerifier(schema);

        List<IExpression> expressions = new ArrayList<>();
        FilterToExpressionConverter converter = new FilterToExpressionConverter();
        for (IColumnFilter filter : filters) {
            converter.setColumnFilter(filter);
            IExpression expr = filter.getMatcher().accept(converter);
            expr.accept(identifierVerifier);

            expressions.add(expr);
        }
        return expressions.size() == 1 ? expressions.get(0) : new LogicalExpression.AND(expressions);
    }

    static class IdentifierVerifier implements IExpressionVisitor {
        private final DataSourceSchema schema;

        IdentifierVerifier(DataSourceSchema schema) {
            this.schema = schema;
        }

        @Override
        public boolean visit(IdentifierExpression expression) {
            IColumn column = schema.getColumnByName(expression.getIdentifier());
            if (column == null) {
                // A special and ugly check.
                // For indexed tags filter, when querying the dimensions, we need to convert its alias to its field name
                // However, when searching spans with tag filters, the schema here does not contain the tags.
                // We need to ignore this case.
                // The ignored tags will be processed later in the trace module.
                if (expression.getIdentifier().startsWith("tags.")) {
                    return true;
                }
                throw new InvalidExpressionException("Unable to find identifier [%s] in data source [%s].",
                                                     expression.getIdentifier(),
                                                     schema.getName());
            }

            // Change to raw name
            expression.setIdentifier(column.getName());

            return true;
        }
    }

    static class FilterToExpressionConverter implements IMatcherVisitor<IExpression> {
        private IdentifierExpression field;

        public void setColumnFilter(IColumnFilter filter) {
            this.field = new IdentifierExpression(filter.getName());
        }

        @Override
        public IExpression visit(EqualMatcher matcher) {
            return new ComparisonExpression.EQ(field, new LiteralExpression(matcher.getPattern()));
        }

        @Override
        public IExpression visit(NotEqualMatcher matcher) {
            return new ComparisonExpression.NE(field, new LiteralExpression(matcher.getPattern()));
        }

        @Override
        public IExpression visit(StringAntPathMatcher matcher) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IExpression visit(StringContainsMatcher matcher) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IExpression visit(StringEndWithMatcher matcher) {
            return new FunctionExpression(null, "endsWith", field, new LiteralExpression(matcher.getPattern()));
        }

        @Override
        public IExpression visit(StringIContainsMatcher matcher) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IExpression visit(StringRegexMatcher matcher) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IExpression visit(StringStartsWithMatcher matcher) {
            return new FunctionExpression(null, "startsWith", field, new LiteralExpression(matcher.getPattern()));
        }

        @Override
        public IExpression visit(BetweenMatcher matcher) {
            return new LogicalExpression.AND(
                new ComparisonExpression.GTE(field, new LiteralExpression(matcher.getLower())),
                new ComparisonExpression.LT(field, new LiteralExpression(matcher.getUpper()))
            );
        }

        @Override
        public IExpression visit(InMatcher inMatcher) {
            return new ComparisonExpression.IN(
                field,
                new ExpressionList(inMatcher.getPattern().stream().map(LiteralExpression::new).collect(Collectors.toList()))
            );
        }

        @Override
        public IExpression visit(GreaterThanMatcher matcher) {
            return new ComparisonExpression.GT(field, new LiteralExpression(matcher.getValue()));
        }

        @Override
        public IExpression visit(GreaterThanOrEqualMatcher matcher) {
            return new ComparisonExpression.GTE(field, new LiteralExpression(matcher.getValue()));
        }

        @Override
        public IExpression visit(LessThanMatcher matcher) {
            return new ComparisonExpression.LT(field, new LiteralExpression(matcher.getValue()));
        }

        @Override
        public IExpression visit(LessThanOrEqualMatcher matcher) {
            return new ComparisonExpression.LTE(field, new LiteralExpression(matcher.getValue()));
        }

        @Override
        public IExpression visit(StringLikeMatcher matcher) {
            return new ComparisonExpression.LIKE(field, new LiteralExpression(matcher.getPattern()));
        }

        @Override
        public IExpression visit(NotMatcher matcher) {
            return new LogicalExpression.NOT(
                matcher.getMatcher().accept(this)
            );
        }
    }
}
