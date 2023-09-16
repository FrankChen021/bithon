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

package org.bithon.component.commons.expression.debug;

import org.bithon.component.commons.expression.BinaryExpression;
import org.bithon.component.commons.expression.IEvaluationContext;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IExpressionVisitor;
import org.bithon.component.commons.expression.IExpressionVisitor2;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/5/4 23:41
 */
public class DebuggableExpression implements IExpression {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(DebuggableExpression.class);

    private final BinaryExpression delegate;

    public DebuggableExpression(BinaryExpression delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getType() {
        return delegate.getType();
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        boolean ret = (boolean) delegate.evaluate(context);
        LOG.info("Expression [{}] evaluates to be {}: left val={}, right val = {}",
                 delegate.toString(),
                 ret,
                 delegate.getLeft().evaluate(context),
                 delegate.getRight().evaluate(context));
        return ret;
    }

    @Override
    public void accept(IExpressionVisitor visitor) {
        delegate.accept(visitor);
    }

    @Override
    public <T> T accept(IExpressionVisitor2<T> visitor) {
        return delegate.accept(visitor);
    }
}
