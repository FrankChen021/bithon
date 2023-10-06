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

import org.bithon.component.commons.utils.ReflectionUtils;

import java.util.Map;

/**
 * @author Frank Chen
 * @date 6/10/23 3:08 pm
 */
public class MapAccessExpression implements IExpression {

    private IExpression map;
    private final String prop;

    public MapAccessExpression(IExpression map, String prop) {
        this.map = map;
        this.prop = prop;
    }

    public IExpression getMap() {
        return map;
    }

    public void setMap(IExpression map) {
        this.map = map;
    }

    public String getProp() {
        return prop;
    }

    @Override
    public String getType() {
        return "[]";
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object evaluate(IEvaluationContext context) {
        Object obj = map.evaluate(context);
        if (obj instanceof Map) {
            return ((Map) obj).get(this.prop);
        }
        return ReflectionUtils.getFieldValue(obj, this.prop);
    }

    @Override
    public void accept(IExpressionVisitor visitor) {
        if (visitor.visit(this)) {
            this.map.accept(visitor);
        }
    }

    @Override
    public <T> T accept(IExpressionVisitor2<T> visitor) {
        return visitor.visit(this);
    }
}
