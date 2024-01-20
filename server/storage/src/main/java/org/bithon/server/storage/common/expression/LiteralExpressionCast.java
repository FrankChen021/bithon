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

package org.bithon.server.storage.common.expression;

import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionVisitor2;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.time.DateTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/1/20 12:33
 */
public class LiteralExpressionCast implements IExpressionVisitor2<IExpression> {
    private final IDataType targetType;

    public LiteralExpressionCast(IDataType targetType) {
        this.targetType = targetType;
    }

    @Override
    public IExpression visit(LiteralExpression expression) {
        if (expression instanceof LiteralExpression.StringLiteral) {
            return castStringTo(expression);
        }
        if (expression instanceof LiteralExpression.DoubleLiteral) {
            return castDoubleTo(expression);
        }
        if (expression instanceof LiteralExpression.BooleanLiteral) {
            return castBooleanTo(expression);
        }
        if (expression instanceof LiteralExpression.LongLiteral) {
            return castLongTo(expression);
        }
        throw new UnsupportedOperationException();
    }

    private IExpression castBooleanTo(LiteralExpression expression) {
        switch (targetType) {
            case STRING:
                return new LiteralExpression.StringLiteral((boolean) expression.getValue() ? "true" : "false");

            case LONG:
                return new LiteralExpression.LongLiteral((boolean) expression.getValue() ? 1 : 0);

            case BOOLEAN:
                return expression;

            default:
                throw new UnsupportedOperationException("Can't cast a boolean value into type of " + targetType);
        }
    }

    private IExpression castLongTo(LiteralExpression expression) {
        switch (targetType) {
            case STRING:
                return new LiteralExpression.StringLiteral(expression.getValue().toString());

            case LONG:
                return expression;

            case DOUBLE:
                return new LiteralExpression.DoubleLiteral((Number) expression.getValue());

            case BOOLEAN:
                return new LiteralExpression.BooleanLiteral(((Number) expression.getValue()).longValue() != 0);

            case DATETIME:
                return new LiteralExpression.DateTimeLiteral(DateTime.toISO8601((long) expression.getValue()));

            default:
                throw new UnsupportedOperationException("Can't cast a boolean value into type of " + targetType);
        }
    }

    private IExpression castDoubleTo(LiteralExpression expression) {
        switch (targetType) {
            case STRING:
                return new LiteralExpression.StringLiteral(expression.getValue().toString());

            case LONG:
                return new LiteralExpression.LongLiteral(((Number) expression.getValue()).longValue());

            case DOUBLE:
                return expression;

            case BOOLEAN:
                return new LiteralExpression.BooleanLiteral(((Number) expression.getValue()).doubleValue() != 0);

            default:
                throw new UnsupportedOperationException("Can't cast a boolean value into type of " + targetType);
        }
    }

    private IExpression castStringTo(LiteralExpression expression) {
        switch (targetType) {
            case LONG:
                return new LiteralExpression.LongLiteral(Long.parseLong(expression.getValue().toString()));

            case DOUBLE:
                return new LiteralExpression.DoubleLiteral(Double.parseDouble(expression.getValue().toString()));

            case BOOLEAN:
                return new LiteralExpression.BooleanLiteral("true".equalsIgnoreCase(expression.getValue()
                                                                                              .toString()));
            case STRING:
                return expression;

            case DATETIME: {
                try {
                    long timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(expression.getValue().toString())
                                                                                .getTime();

                    return new LiteralExpression.DateTimeLiteral(DateTime.toISO8601(timestamp));
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }

            default:
                throw new UnsupportedOperationException("Can't cast a boolean value into type of " + targetType);
        }
    }

}
