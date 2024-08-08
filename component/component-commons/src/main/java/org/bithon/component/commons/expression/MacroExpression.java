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

/**
 * @author Frank Chen
 * @date 26/8/23 9:37 pm
 */
public class MacroExpression implements IExpression {
    private final String macro;
    private IDataType dataType;

    public MacroExpression(String macro) {
        this.macro = macro;
    }

    public String getMacro() {
        return macro;
    }

    @Override
    public IDataType getDataType() {
        return dataType;
    }

    public void setDataType(IDataType dataType) {
        this.dataType = dataType;
    }

    @Override
    public String getType() {
        return "macro";
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        return context.get(macro);
    }

    @Override
    public void accept(IExpressionInDepthVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <T> T accept(IExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
