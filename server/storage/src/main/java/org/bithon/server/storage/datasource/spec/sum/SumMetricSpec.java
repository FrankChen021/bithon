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

package org.bithon.server.storage.datasource.spec.sum;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.query.ast.SimpleAggregateExpression;
import org.bithon.server.storage.datasource.query.ast.SimpleAggregateExpressions;
import org.bithon.server.storage.datasource.spec.IMetricSpec;
import org.bithon.server.storage.datasource.spec.IMetricSpecVisitor;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/9/4 20:17
 */
public abstract class SumMetricSpec implements IMetricSpec {

    @Getter
    protected final String name;

    @Getter
    protected final String field;

    @Getter
    protected final String displayText;

    protected final SimpleAggregateExpression aggregateExpression;

    public SumMetricSpec(String name,
                         String field,
                         String displayText) {
        this.name = name;
        this.field = field;
        this.displayText = displayText;
        this.aggregateExpression = new SimpleAggregateExpressions.SumAggregateExpression(name);
    }

    @JsonIgnore
    @Override
    public SimpleAggregateExpression getAggregateExpression() {
        return aggregateExpression;
    }

    @Override
    public final <T> T accept(IMetricSpecVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
