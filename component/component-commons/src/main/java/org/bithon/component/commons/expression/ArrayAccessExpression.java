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

package org.bithon.component.commons.expression;

import org.bithon.component.commons.utils.StringUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author Frank Chen
 * @date 31/7/23 8:20 pm
 */
public class ArrayAccessExpression implements IExpression {

    private IExpression array;
    private final int index;

    public ArrayAccessExpression(IExpression array, int index) {
        this.array = array;
        this.index = index;
    }

    public IExpression getArray() {
        return array;
    }

    public void setArray(IExpression array) {
        this.array = array;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String getType() {
        return "[]";
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        Object obj = array.evaluate(context);
        if (obj instanceof char[]) {
            if (index < ((char[]) obj).length) {
                return ((char[]) obj)[index];
            }
            throw new RuntimeException(StringUtils.format("Failed evaluate expression: %s. Index [%d] is out of length [%d].", this.serializeToText(), index, ((char[]) obj).length));
        }
        if (obj instanceof byte[]) {
            if (index < ((byte[]) obj).length) {
                return ((byte[]) obj)[index];
            }
            throw new RuntimeException(StringUtils.format("Failed evaluate expression: %s. Index [%d] is out of length [%d].", this.serializeToText(), index, ((byte[]) obj).length));
        }
        if (obj instanceof short[]) {
            if (index < ((short[]) obj).length) {
                return ((short[]) obj)[index];
            }
            throw new RuntimeException(StringUtils.format("Failed evaluate expression: %s. Index [%d] is out of length [%d].", this.serializeToText(), index, ((short[]) obj).length));
        }
        if (obj instanceof int[]) {
            if (index < ((int[]) obj).length) {
                return ((int[]) obj)[index];
            }
            throw new RuntimeException(StringUtils.format("Failed evaluate expression: %s. Index [%d] is out of length [%d].", this.serializeToText(), index, ((int[]) obj).length));
        }
        if (obj instanceof long[]) {
            if (index < ((long[]) obj).length) {
                return ((long[]) obj)[index];
            }
            throw new RuntimeException(StringUtils.format("Failed evaluate expression: %s. Index [%d] is out of length [%d].", this.serializeToText(), index, ((long[]) obj).length));
        }
        if (obj instanceof float[]) {
            if (index < ((float[]) obj).length) {
                return ((float[]) obj)[index];
            }
            throw new RuntimeException(StringUtils.format("Failed evaluate expression: %s. Index [%d] is out of length [%d].", this.serializeToText(), index, ((float[]) obj).length));
        }
        if (obj instanceof double[]) {
            if (index < ((double[]) obj).length) {
                return ((double[]) obj)[index];
            }
            throw new RuntimeException(StringUtils.format("Failed evaluate expression: %s. Index [%d] is out of length [%d].", this.serializeToText(), index, ((double[]) obj).length));
        }
        if (obj instanceof Object[]) {
            if (index < ((Object[]) obj).length) {
                return ((Object[]) obj)[index];
            }
            throw new RuntimeException(StringUtils.format("Failed evaluate expression: %s. Index [%d] is out of length [%d].", this.serializeToText(), index, ((Object[]) obj).length));
        }
        if (obj instanceof List) {
            if (index < ((List<?>) obj).size()) {
                return ((List<?>) obj).get(index);
            }
            throw new RuntimeException(StringUtils.format("Failed evaluate expression: %s. Index [%d] is out of length [%d].", this.serializeToText(), index, ((List<?>) obj).size()));
        }
        if (obj instanceof Collection) {
            int size = ((Collection<?>) obj).size();
            if (index < size) {
                Iterator<?> iterator = ((Collection<?>) obj).iterator();
                for (int i = 0; i < index; i++) {
                    iterator.next();
                }
                return iterator.next();
            }
            throw new RuntimeException(StringUtils.format("Failed evaluate expression: %s. Index [%d] is out of length [%d].", this.serializeToText(), index, size));
        }

        throw new RuntimeException(StringUtils.format("Failed to evaluate expression: %s. It does not return an object of array, but type of [%s]", array.serializeToText(), obj.getClass().getName()));
    }

    @Override
    public void accept(IExpressionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <T> T accept(IExpressionVisitor2<T> visitor) {
        return visitor.visit(this);
    }
}
