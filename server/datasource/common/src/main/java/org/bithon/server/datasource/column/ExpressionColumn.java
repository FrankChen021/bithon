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

package org.bithon.server.datasource.column;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.datasource.DefaultSchema;
import org.bithon.server.datasource.aggregator.NumberAggregator;
import org.bithon.server.datasource.query.ast.ExpressionNode;
import org.bithon.server.datasource.query.ast.Selector;


/**
 * @author frankchen
 */
public class ExpressionColumn implements IColumn {

    @Getter
    private final String name;

    @Getter
    private final String alias;

    @Getter
    private final String expression;

    @Getter
    private final IDataType valueType;

    @Setter
    private DefaultSchema schema;

    @JsonCreator
    public ExpressionColumn(@JsonProperty("name") @NotNull String name,
                            @JsonProperty("alias") @Nullable String alias,
                            @JsonProperty("expression") @NotNull String expression,
                            @JsonProperty("valueType") @Nullable String valueType) {
        this.name = name;
        this.alias = alias == null ? name : alias;
        this.expression = Preconditions.checkArgumentNotNull("expression", expression).trim();
        this.valueType = "long".equalsIgnoreCase(valueType) ? IDataType.LONG : IDataType.DOUBLE;
    }

    @Override
    public NumberAggregator createAggregator() {
        return null;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ExpressionColumn) {
            return this.name.equals(((ExpressionColumn) obj).name);
        } else {
            return false;
        }
    }

    @Override
    public Selector toSelector() {
        return new Selector(new ExpressionNode(schema, this.expression), this.name, this.valueType);
    }

    @JsonIgnore
    @Override
    public IDataType getDataType() {
        return this.valueType;
    }
}
