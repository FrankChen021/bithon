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
import org.bithon.component.commons.expression.ExpressionList;
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

    private final IExpression partitionBy;
    private final IExpression orderBy;
    private final WindowFunctionFrame frame;

    public WindowFunctionExpression(String functionName,
                                    List<IExpression> args,
                                    IExpression partitionBy,
                                    IExpression orderBy,
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
            if (partitionBy != null) {
                serializer.append("PARTITION BY ");
                serializer.serialize(partitionBy);
            }
            if (orderBy != null) {
                serializer.append(" ORDER BY ");
                serializer.serialize(orderBy);
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
        private IExpression partitionBy;
        private IExpression orderBy;
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
            this.partitionBy = partitionBy;
            return this;
        }

        public Builder partitionBy(IExpression... partitionBys) {
            this.partitionBy = new ExpressionList(partitionBys);
            return this;
        }

        public Builder orderBy(IExpression orderBy) {
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
