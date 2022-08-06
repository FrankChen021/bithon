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

package org.bithon.server.storage.datasource.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.component.commons.utils.Preconditions;

import javax.validation.constraints.NotNull;

/**
 * @author frank.chen
 * @date 2022/8/6 17:37
 */
public abstract class SimpleAggregator implements IQueryableAggregator {

    @Getter
    protected final String name;

    @Getter
    protected final String field;

    public SimpleAggregator(String name,
                            String field) {
        this.name = Preconditions.checkArgumentNotNull("name", name);
        this.field = field == null ? name : field;
    }

    public abstract String getAggregator();

    @Override
    public <T> T accept(IQueryableAggregatorVisitor<T> visitor) {
        return visitor.visit(this);
    }

    public static class MinAggregator extends SimpleAggregator {
        public static final String TYPE = "min";

        @JsonCreator
        public MinAggregator(@JsonProperty("name") @NotNull String name,
                             @JsonProperty("field") @NotNull String field) {
            super(name, field);
        }

        @Override
        public String getAggregator() {
            return TYPE;
        }
    }

    public static class MaxAggregator extends SimpleAggregator {
        public static final String TYPE = "max";

        @JsonCreator
        public MaxAggregator(@JsonProperty("name") @NotNull String name,
                             @JsonProperty("field") @NotNull String field) {
            super(name, field);
        }

        @Override
        public String getAggregator() {
            return TYPE;
        }
    }

    public static class SumAggregator extends SimpleAggregator {
        public static final String TYPE = "sum";

        @JsonCreator
        public SumAggregator(@JsonProperty("name") @NotNull String name,
                             @JsonProperty("field") @NotNull String field) {
            super(name, field);
        }

        @Override
        public String getAggregator() {
            return TYPE;
        }
    }

    public static class AvgAggregator extends SimpleAggregator {
        public static final String TYPE = "avg";

        @JsonCreator
        public AvgAggregator(@JsonProperty("name") @NotNull String name,
                             @JsonProperty("field") @NotNull String field) {
            super(name, field);
        }

        @Override
        public String getAggregator() {
            return TYPE;
        }
    }
}
