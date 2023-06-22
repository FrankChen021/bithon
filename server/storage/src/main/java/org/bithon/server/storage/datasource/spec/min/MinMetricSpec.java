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

package org.bithon.server.storage.datasource.spec.min;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.bithon.server.storage.datasource.query.ast.SimpleAggregateExpression;
import org.bithon.server.storage.datasource.query.ast.SimpleAggregateExpressions;
import org.bithon.server.storage.datasource.spec.IMetricSpec;
import org.bithon.server.storage.datasource.spec.IMetricSpecVisitor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/16
 */
public abstract class MinMetricSpec implements IMetricSpec {

    @Getter
    protected final String name;

    @Getter
    private final String alias;

    @Getter
    protected final String field;

    @Getter
    protected final String displayText;

    protected final SimpleAggregateExpression aggregateExpression;

    @JsonCreator
    public MinMetricSpec(String name,
                         String alias,
                         String field,
                         String displayText) {
        this.name = name;
        this.alias = alias == null ? name : alias;
        this.field = field;
        this.displayText = displayText;

        // For IMetricSpec, the `name` property is the right text mapped a column in the underlying database,
        // So the two parameters of the following ctor are all `name` properties
        this.aggregateExpression = new SimpleAggregateExpressions.MinAggregateExpression(name);
    }

    @Override
    public <T> T accept(IMetricSpecVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @JsonIgnore
    @Override
    public SimpleAggregateExpression getAggregateExpression() {
        return aggregateExpression;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
