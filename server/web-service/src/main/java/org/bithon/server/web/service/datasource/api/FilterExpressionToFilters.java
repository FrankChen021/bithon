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

import org.bithon.component.commons.expression.BinaryExpression;
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.matcher.GreaterThanMatcher;
import org.bithon.server.commons.matcher.GreaterThanOrEqualMatcher;
import org.bithon.server.commons.matcher.InMatcher;
import org.bithon.server.commons.matcher.LessThanMatcher;
import org.bithon.server.commons.matcher.LessThanOrEqualMatcher;
import org.bithon.server.commons.matcher.NotEqualMatcher;
import org.bithon.server.commons.matcher.StringEqualMatcher;
import org.bithon.server.commons.matcher.StringLikeMatcher;
import org.bithon.server.storage.common.expression.FilterExpressionASTFactory;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.filter.ColumnFilter;
import org.bithon.server.storage.datasource.filter.IColumnFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/5/6 23:49
 */
public class FilterExpressionToFilters {

    public static List<IColumnFilter> toFilter(DataSourceSchema schema, String filterExpression, List<IColumnFilter> otherFilters) {
        if (StringUtils.isEmpty(filterExpression)) {
            return otherFilters == null ? Collections.emptyList() : otherFilters;
        }

        IExpression expressionAST = FilterExpressionASTFactory.create(filterExpression);
        Visitor v = new Visitor(schema);
        expressionAST.accept(v);
        List<IColumnFilter> filters = v.filters;
        if (CollectionUtils.isNotEmpty(otherFilters)) {
            filters.addAll(otherFilters);
        }
        return filters;
    }

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
                                         new StringEqualMatcher((String) ((LiteralExpression) expression.getRight()).getValue())));
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
    }
}
