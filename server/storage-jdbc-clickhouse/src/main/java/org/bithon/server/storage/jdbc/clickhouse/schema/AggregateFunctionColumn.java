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

package org.bithon.server.storage.jdbc.clickhouse.schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.bithon.component.commons.Experimental;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.function.IFunction;
import org.bithon.component.commons.expression.function.builtin.AggregateFunction;
import org.bithon.server.storage.datasource.column.IColumn;
import org.bithon.server.storage.datasource.query.ast.Expression;
import org.bithon.server.storage.datasource.query.ast.Selector;

import java.util.List;

/**
 * maps to ClickHouse's AggregateFunctionColumn
 *
 * @author frank.chen021@outlook.com
 * @date 22/11/24 1:00 pm
 */
@Experimental
public class AggregateFunctionColumn implements IColumn {
    private final FunctionExpression functionExpression;
    @Getter
    private String name;

    @Getter
    private String alias;

    @Getter
    private IDataType dataType;

    @Getter
    @Setter
    public String aggregator;

    @JsonCreator
    public AggregateFunctionColumn(@JsonProperty("name") String name,
                                   @JsonProperty("alias") String alias,
                                   @JsonProperty("aggregator") String aggregator,
                                   @JsonProperty("dataType") IDataType dataType) {
        this.name = name;
        this.alias = alias;
        this.dataType = dataType;
        this.aggregator = aggregator;
        this.functionExpression = FunctionExpression.create("sum".equals(aggregator) ? SumMergeFunction.INSTANCE : CountMergeFunction.INSTANCE, name);
    }

    @Override
    public Selector toSelector() {
        return new Selector(new Expression(functionExpression), getName());
    }

    @Override
    public IExpression createAggregateFunctionExpression(IFunction function) {
        if (function instanceof AggregateFunction.Sum) {
            return new FunctionExpression(SumMergeFunction.INSTANCE, new IdentifierExpression(name));
        } else if (function instanceof AggregateFunction.Count) {
            return new FunctionExpression(CountMergeFunction.INSTANCE, new IdentifierExpression(name));
        } else {
            return new FunctionExpression(CountMergeFunction.INSTANCE, new IdentifierExpression(name));
        }
    }

    public static class SumMergeFunction extends AggregateFunction.Sum {
        public static final AggregateFunction INSTANCE = new SumMergeFunction();

        public SumMergeFunction() {
            super("sumMerge");
        }

        @Override
        public Object evaluate(List<Object> args) {
            return null;
        }
    }

    public static class CountMergeFunction extends AggregateFunction.Count {
        public static final AggregateFunction INSTANCE = new CountMergeFunction();

        public CountMergeFunction() {
            super("countMerge");
        }

        @Override
        public Object evaluate(List<Object> args) {
            return null;
        }
    }
}
