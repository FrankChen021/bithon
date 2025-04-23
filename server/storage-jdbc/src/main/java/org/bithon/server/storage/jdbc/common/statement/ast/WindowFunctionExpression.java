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

package org.bithon.server.storage.jdbc.common.statement.ast;


import lombok.Getter;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.function.builtin.AggregateFunction;
import org.bithon.component.commons.expression.serialization.ExpressionSerializer;

import java.util.List;

/**
 * example:
 * FIRST_VALUE("activeThreads") OVER (PARTITION BY UNIX_TIMESTAMP("timestamp") / 600 * 600 ORDER BY "timestamp")
 *
 * @author frank.chen021@outlook.com
 * @date 21/4/25 9:22 pm
 */
@Getter
public class WindowFunctionExpression extends FunctionExpression {
    static class WindowFunctionAggregator extends AggregateFunction {
        public WindowFunctionAggregator(String name) {
            super(name);
        }

        @Override
        public void validateArgs(List<IExpression> args) {
        }

        @Override
        public Object evaluate(List<Object> args) {
            throw new UnsupportedOperationException();
        }
    }

    private final IExpression[] partitionBy;
    private final OrderByElement[] orderBy;
    private final WindowFunctionFrame frame;

    public WindowFunctionExpression(String functionName,
                                    List<IExpression> args,
                                    IExpression[] partitionBy,
                                    OrderByElement[] orderBy,
                                    IExpression frame) {
        super(new WindowFunctionAggregator(functionName), args);
        this.partitionBy = partitionBy;
        this.orderBy = orderBy;
        this.frame = (WindowFunctionFrame) frame;
    }

    @Override
    public void serializeToText(ExpressionSerializer serializer) {
        super.serializeToText(serializer);
        serializer.append(" OVER (");
        {
            if (partitionBy != null && partitionBy.length > 0) {
                serializer.append("PARTITION BY ");
                for (int i = 0; i < partitionBy.length; i++) {
                    if (i > 0) {
                        serializer.append(", ");
                    }
                    partitionBy[i].serializeToText(serializer);
                }
                serializer.append(" ");
            }

            if (orderBy != null && orderBy.length > 0) {
                serializer.append("ORDER BY ");
                for (int i = 0; i < orderBy.length; i++) {
                    if (i > 0) {
                        serializer.append(", ");
                    }
                    orderBy[i].serializeToText(serializer);
                }
            }

            if (frame != null) {
                serializer.append(" ");
                serializer.serialize(frame);
            }
        }
        serializer.append(")");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private List<IExpression> args;
        private IExpression[] partitionBy;
        private OrderByElement[] orderBy;
        private WindowFunctionFrame frame;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder args(List<IExpression> args) {
            this.args = args;
            return this;
        }

        public Builder partitionBy(IExpression partitionBy) {
            this.partitionBy = partitionBy == null ? null : new IExpression[]{partitionBy};
            return this;
        }

        public Builder partitionBy(IExpression... partitionBys) {
            this.partitionBy = partitionBys;
            return this;
        }

        public Builder orderBy(OrderByElement orderBy) {
            this.orderBy = orderBy == null ? null : new OrderByElement[]{orderBy};
            return this;
        }

        public Builder orderBy(OrderByElement... orderBy) {
            this.orderBy = orderBy;
            return this;
        }

        public Builder frame(WindowFunctionFrame frame) {
            this.frame = frame;
            return this;
        }

        public WindowFunctionExpression build() {
            return new WindowFunctionExpression(name, args, partitionBy, orderBy, frame);
        }
    }
}
