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

import org.bithon.component.commons.expression.serialization.ExpressionSerializer;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/7 20:17
 */
public abstract class BinaryExpression implements IExpression {

    /**
     * Don't change these property names because they're used in manual deserializer
     */
    protected final String type;
    protected IExpression lhs;
    protected IExpression rhs;

    protected BinaryExpression(String type, IExpression lhs, IExpression rhs) {
        this.type = type;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public IExpression getLhs() {
        return lhs;
    }

    public IExpression getRhs() {
        return rhs;
    }

    public void setLhs(IExpression lhs) {
        this.lhs = lhs;
    }

    public void setRhs(IExpression rhs) {
        this.rhs = rhs;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void serializeToText(ExpressionSerializer serializer) {
        serializer.serialize(this);
    }
}
