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
 * @date 2023/4/7 20:16
 */
public class IdentifierExpression implements IExpression {

    public static IdentifierExpression of(String identifier) {
        return new IdentifierExpression(identifier);
    }

    public static IdentifierExpression of(String identifier, IDataType argType) {
        IdentifierExpression expr = new IdentifierExpression(identifier);
        expr.setDataType(argType);
        return expr;
    }

    /**
     * NOT final to allow AST optimization
     */
    private String qualifier;
    private String identifier;
    private IDataType dataType;

    public IdentifierExpression(String identifier) {
        setIdentifier(identifier);
    }

    public IdentifierExpression(String identifier, IDataType dataType) {
        setIdentifier(identifier);
        this.dataType = dataType;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public void setIdentifier(String identifier) {
        int idx = identifier.indexOf('.');
        if (idx > 0) {
            this.qualifier = identifier.substring(0, idx);
            this.identifier = identifier.substring(idx + 1);
        } else {
            this.identifier = identifier;
        }
    }

    public String getQualifier() {
        return qualifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String getType() {
        return "identifier";
    }

    public boolean isQualified() {
        return this.qualifier != null;
    }

    @Override
    public IDataType getDataType() {
        return dataType;
    }

    public IdentifierExpression setDataType(IDataType dataType) {
        this.dataType = dataType;
        return this;
    }

    @Override
    public void accept(IExpressionInDepthVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public <T> T accept(IExpressionVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void serializeToText(ExpressionSerializer serializer) {
        serializer.serialize(this);
    }

    @Override
    public Object evaluate(IEvaluationContext context) {
        return context.get(identifier);
    }

    @Override
    public String toString() {
        return identifier;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IdentifierExpression)) {
            return false;
        }
        IdentifierExpression that = (IdentifierExpression) obj;
        return this.identifier.equals(that.identifier) &&
               ((this.qualifier == null && that.qualifier == null) ||
                (this.qualifier != null && this.qualifier.equals(that.qualifier))) &&
               ((this.dataType == null && that.dataType == null) ||
                (this.dataType != null && this.dataType.equals(that.dataType)));
    }
}
