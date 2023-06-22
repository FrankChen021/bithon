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

package org.bithon.server.storage.datasource.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.aggregator.NumberAggregator;
import org.bithon.server.storage.datasource.query.ast.Expression;
import org.bithon.server.storage.datasource.query.ast.ResultColumn;
import org.bithon.server.storage.datasource.query.ast.SimpleAggregateExpression;
import org.bithon.server.storage.datasource.typing.IDataType;

import javax.validation.constraints.NotNull;

/**
 * @author frankchen
 */
public class PostAggregatorMetricSpec implements IMetricSpec {
    @Getter
    private final String name;

    @Getter
    private final String displayText;

    @Getter
    private final String expression;

    @Getter
    private final IDataType valueType;

    @JsonCreator
    public PostAggregatorMetricSpec(@JsonProperty("name") @NotNull String name,
                                    @JsonProperty("displayText") @NotNull String displayText,
                                    @JsonProperty("expression") @NotNull String expression,
                                    @JsonProperty("valueType") @NotNull String valueType) {
        this.name = name;
        this.displayText = displayText;
        this.expression = Preconditions.checkArgumentNotNull("expression", expression).trim();
        this.valueType = "long".equalsIgnoreCase(valueType) ? IDataType.LONG : IDataType.DOUBLE;
    }

    @JsonIgnore
    @Override
    public String getType() {
        return POST;
    }

    @Override
    public String getField() {
        return null;
    }

    @Override
    public <T> T accept(IMetricSpecVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public NumberAggregator createAggregator() {
        return null;
    }

    @JsonIgnore
    @Override
    public SimpleAggregateExpression getAggregateExpression() {
        return null;
    }

    @Override
    public void setOwner(DataSourceSchema dataSource) {
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PostAggregatorMetricSpec) {
            return this.name.equals(((PostAggregatorMetricSpec) obj).name);
        } else {
            return false;
        }
    }

    @Override
    public ResultColumn getResultColumn() {
        return new ResultColumn(new Expression(this.expression), this.name);
    }

    @Override
    public IDataType getDataType() {
        return this.valueType;
    }
}
