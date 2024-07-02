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

package org.bithon.server.storage.datasource.column.aggregatable.sum;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.server.storage.datasource.aggregator.DoubleSumAggregator;
import org.bithon.server.storage.datasource.aggregator.NumberAggregator;

import javax.annotation.Nullable;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/23
 */
public class AggregateDoubleSumColumn extends AggregateSumColumn {

    @JsonCreator
    public AggregateDoubleSumColumn(@JsonProperty("name") @NotNull String name,
                                    @JsonProperty("alias") @Nullable String alias) {
        super(name, alias);
    }

    @Override
    public IDataType getDataType() {
        return IDataType.DOUBLE;
    }

    @Override
    public NumberAggregator createAggregator() {
        return new DoubleSumAggregator();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AggregateDoubleSumColumn) {
            return this.name.equals(((AggregateDoubleSumColumn) obj).name);
        } else {
            return false;
        }
    }
}
