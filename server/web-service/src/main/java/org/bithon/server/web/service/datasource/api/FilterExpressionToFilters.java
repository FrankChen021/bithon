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

import org.bithon.component.commons.expression.CollectionExpression;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
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
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.filter.IColumnFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/5/6 23:49
 */
public class FilterExpressionToFilters {

    public static IExpression toFilter(DataSourceSchema schema, String filterExpression, List<IColumnFilter> otherFilters) {
        if (StringUtils.isEmpty(filterExpression)) {
            return otherFilters == null ? null : toExpression(otherFilters);
        }

        IExpression expression = ExpressionASTBuilder.build(filterExpression, null);
        if (otherFilters != null) {
            return new LogicalExpression.AND(expression, toExpression(otherFilters));
        } else {
            return expression;
        }
    }

    private static IExpression toExpression(List<IColumnFilter> filters) {
        List<IExpression> expressions = new ArrayList<>();
        FilterToExpression converter = new FilterToExpression();
        for (IColumnFilter filter : filters) {
            converter.setColumnFilter(filter);
            expressions.add(filter.getMatcher().accept(converter));
        }
        return new LogicalExpression.AND(expressions);
    }

    static class FilterToExpression implements IMatcherVisitor<IExpression> {
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
            return new ComparisonExpression.NE(field, new LiteralExpression(matcher.getValue()));
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
                new CollectionExpression(inMatcher.getPattern().stream().map(LiteralExpression::new).collect(Collectors.toList()))
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

    /*
    static class Visitor implements IExpressionVisitor<Void> {
        private final List<IColumnFilter> filters = new ArrayList<>();
        private final DataSourceSchema schema;

        public Visitor(DataSourceSchema schema) {
            this.schema = schema;
        }

        @Override
        public Void visit(LogicalExpression expression) {
            if (expression instanceof LogicalExpression.OR) {
                throw new UnsupportedOperationException("OR operator is not supported now.");
            }

            if (expression instanceof LogicalExpression.NOT) {
                IExpression operand = expression.getOperands().get(0);
                Visitor v = new Visitor(this.schema);
                operand.accept(v);
                IColumnFilter filter = v.filters.get(0);
                filters.add(new ColumnFilter(filter.getName(), "alias", new NotMatcher(filter.getMatcher())));
                return null;
            }

            for (IExpression operand : expression.getOperands()) {
                operand.accept(this);
            }
            return null;
        }

        private void checkBinaryExpression(BinaryExpression binaryExpression,
                                           Class<? extends IExpression> rightExpressionType) {
            IExpression left = binaryExpression.getLeft();
            if (!(left instanceof IdentifierExpression)) {
                throw new UnsupportedOperationException("Expression at left side must be a field");
            }

            IExpression right = binaryExpression.getRight();
            if (!rightExpressionType.isAssignableFrom(right.getClass())) {
                throw new UnsupportedOperationException(StringUtils.format(
                    "Expression at right side must be type of [%s], but is [%s].",
                    rightExpressionType.getSimpleName(),
                    right.getClass().getSimpleName()));
            }
        }

        @Override
        public Void visit(BinaryExpression.IN expression) {
            checkBinaryExpression(expression, ExpressionList.class);

            Set<String> patterns = new HashSet<>();
            List<IExpression> expressionList = ((ExpressionList) expression.getRight()).getExpressionList();
            for (IExpression expr : expressionList) {
                if (!(expr instanceof LiteralExpression)) {
                    throw new UnsupportedOperationException(StringUtils.format(
                        "Expression [%s] of the IN operator must be a constant",
                        expr.toString()));
                }
                patterns.add(expr.toString());
            }

            filters.add(new ColumnFilter(((IdentifierExpression) expression.getLeft()).getIdentifier(),
                                         "alias",
                                         new InMatcher(patterns)));
            return null;
        }

        @Override
        public Void visit(BinaryExpression.EQ expression) {
            checkBinaryExpression(expression, LiteralExpression.class);

            filters.add(new ColumnFilter(((IdentifierExpression) expression.getLeft()).getIdentifier(),
                                         "alias",
                                         new EqualMatcher(((LiteralExpression) expression.getRight()).getValue())));
            return null;
        }

        @Override
        public Void visit(BinaryExpression.GT expression) {
            checkBinaryExpression(expression, LiteralExpression.class);

            IColumn column = schema.getColumnByName(((IdentifierExpression) expression.getLeft()).getIdentifier());
            Preconditions.checkNotNull(column,
                                       "Column [%s] can not be found in schema [%s].",
                                       ((IdentifierExpression) expression.getLeft()).getIdentifier(), schema.getName());

            filters.add(new ColumnFilter(((IdentifierExpression) expression.getLeft()).getIdentifier(),
                                         "alias",
                                         new GreaterThanMatcher(((LiteralExpression) expression.getRight()).getValue())));

            return null;
        }

        @Override
        public Void visit(BinaryExpression.GTE expression) {
            checkBinaryExpression(expression, LiteralExpression.class);

            IColumn column = schema.getColumnByName(((IdentifierExpression) expression.getLeft()).getIdentifier());
            Preconditions.checkNotNull(column,
                                       "Column [%s] can not be found in schema [%s].",
                                       ((IdentifierExpression) expression.getLeft()).getIdentifier(), schema.getName());

            filters.add(new ColumnFilter(((IdentifierExpression) expression.getLeft()).getIdentifier(),
                                         "alias",
                                         new GreaterThanOrEqualMatcher(((LiteralExpression) expression.getRight()).getValue())));
            return null;
        }

        @Override
        public Void visit(BinaryExpression.LT expression) {
            checkBinaryExpression(expression, LiteralExpression.class);

            IColumn column = schema.getColumnByName(((IdentifierExpression) expression.getLeft()).getIdentifier());
            Preconditions.checkNotNull(column,
                                       "Column [%s] can not be found in schema [%s].",
                                       ((IdentifierExpression) expression.getLeft()).getIdentifier(), schema.getName());

            filters.add(new ColumnFilter(((IdentifierExpression) expression.getLeft()).getIdentifier(),
                                         "alias",
                                         new LessThanMatcher(((LiteralExpression) expression.getRight()).getValue())));
            return null;
        }

        @Override
        public Void visit(BinaryExpression.LTE expression) {
            checkBinaryExpression(expression, LiteralExpression.class);

            IColumn column = schema.getColumnByName(((IdentifierExpression) expression.getLeft()).getIdentifier());
            Preconditions.checkNotNull(column,
                                       "Column [%s] can not be found in schema [%s].",
                                       ((IdentifierExpression) expression.getLeft()).getIdentifier(), schema.getName());

            filters.add(new ColumnFilter(((IdentifierExpression) expression.getLeft()).getIdentifier(),
                                         "alias",
                                         new LessThanOrEqualMatcher(((LiteralExpression) expression.getRight()).getValue())));
            return null;
        }

        @Override
        public Void visit(BinaryExpression.NE expression) {
            checkBinaryExpression(expression, LiteralExpression.class);

            IColumn column = schema.getColumnByName(((IdentifierExpression) expression.getLeft()).getIdentifier());
            Preconditions.checkNotNull(column,
                                       "Column [%s] can not be found in schema [%s].",
                                       ((IdentifierExpression) expression.getLeft()).getIdentifier(), schema.getName());

            filters.add(new ColumnFilter(((IdentifierExpression) expression.getLeft()).getIdentifier(),
                                         "alias",
                                         new NotEqualMatcher(((LiteralExpression) expression.getRight()).getValue())));
            return null;
        }

        @Override
        public Void visit(BinaryExpression.LIKE expression) {
            checkBinaryExpression(expression, LiteralExpression.class);

            IColumn column = schema.getColumnByName(((IdentifierExpression) expression.getLeft()).getIdentifier());
            Preconditions.checkNotNull(column,
                                       "Column [%s] can not be found in schema [%s].",
                                       ((IdentifierExpression) expression.getLeft()).getIdentifier(), schema.getName());

            Preconditions.checkIfTrue(expression.getRight() instanceof LiteralExpression,
                                      "Right expression of LIKE operator must be a literal.");
            Preconditions.checkIfTrue(((LiteralExpression) expression.getRight()).getValue() instanceof String,
                                      "right expression of LIKE operator must be a STRING literal.");

            filters.add(new ColumnFilter(((IdentifierExpression) expression.getLeft()).getIdentifier(),
                                         "alias",
                                         new StringLikeMatcher((String) ((LiteralExpression) expression.getRight()).getValue())));

            return null;
        }
    }*/
}
