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

package org.bithon.server.web.service.tracing.service;

import org.bithon.component.commons.expression.BinaryExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.server.commons.matcher.StringEqualMatcher;
import org.bithon.server.storage.common.expression.FilterExpressionASTFactory;
import org.bithon.server.storage.metrics.DimensionFilter;
import org.bithon.server.storage.metrics.IFilter;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/5/6 23:49
 */
class FilterExpressionToFilters {

    static List<IFilter> toFilter(String filterExpression) {
        if (StringUtils.isEmpty(filterExpression)) {
            return Collections.emptyList();
        }

        IExpression expressionAST = FilterExpressionASTFactory.create(filterExpression);
        Visitor v = new Visitor();
        expressionAST.accept(v);
        return v.filters;
    }

    static class Visitor implements IExpressionVisitor<Void> {
        private final List<IFilter> filters = new ArrayList<>();

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

        @Override
        public Void visit(BinaryExpression.EQ expression) {
            IExpression left = expression.getLeft();
            if (!(left instanceof IdentifierExpression)) {
                throw new UnsupportedOperationException("Expression at left side must be a field");
            }

            IExpression right = expression.getRight();
            if (!(right instanceof LiteralExpression)) {
                throw new UnsupportedOperationException("Expression at right side must be a constant");
            }

            filters.add(new DimensionFilter(((IdentifierExpression) left).getIdentifier(),
                                            new StringEqualMatcher((String) ((LiteralExpression) right).getValue())));
            return null;
        }

        @Override
        public Void visit(BinaryExpression.GT expression) {
            return IExpressionVisitor.super.visit(expression);
        }

        @Override
        public Void visit(BinaryExpression.GTE expression) {
            return IExpressionVisitor.super.visit(expression);
        }

        @Override
        public Void visit(BinaryExpression.LT expression) {
            return IExpressionVisitor.super.visit(expression);
        }

        @Override
        public Void visit(BinaryExpression.LTE expression) {
            return IExpressionVisitor.super.visit(expression);
        }

        @Override
        public Void visit(BinaryExpression.NE expression) {
            return IExpressionVisitor.super.visit(expression);
        }
    }
}