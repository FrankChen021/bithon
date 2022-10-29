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
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.api.IQueryStageAggregator;
import org.bithon.server.storage.datasource.api.QueryStageAggregators;
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
    protected final String field;

    @Getter
    protected final String displayText;

    @Getter
    protected final String unit;

    @Getter
    protected final boolean visible;
    protected final IQueryStageAggregator queryStageAggregator;

    @JsonCreator
    public MinMetricSpec(String name,
                         String field,
                         String displayText,
                         String unit,
                         Boolean visible) {
        this.name = name;
        this.field = field;
        this.displayText = displayText;
        this.unit = unit;
        this.visible = visible == null ? true : visible;

        // For IMetricSpec, the `name` property is the right text mapped a column in underlying database,
        // So the two parameters of following ctor are all `name` properties
        this.queryStageAggregator = new QueryStageAggregators.MinAggregator(name, name);
    }

    @Override
    public void setOwner(DataSourceSchema dataSource) {
    }

    @Override
    public <T> T accept(IMetricSpecVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @JsonIgnore
    @Override
    public IQueryStageAggregator getQueryAggregator() {
        return queryStageAggregator;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
