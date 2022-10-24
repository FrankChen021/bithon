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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * count(distinct )
 *
 * @author Frank Chen
 * @date 1/11/21 2:37 pm
 */

public class QueryStageAggregators {
    public static class CardinalityAggregator extends AbstractQueryStageAggregator {
        public static final String TYPE = "cardinality";

        @JsonCreator
        public CardinalityAggregator(@JsonProperty("name") @NotNull String name,
                                     @JsonProperty("field") @NotNull String dimension) {
            super(dimension, name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    abstract static class AbstractQueryStageAggregator implements IQueryStageAggregator {
        @Getter
        protected final String field;

        // metric name and final output name
        @Getter
        protected final String name;

        /**
         * aggregator type
         */
        @JsonIgnore
        @Getter
        protected final String type;

        public AbstractQueryStageAggregator(String field, String name, String aggregator) {
            this.field = field == null ? name : field;
            this.name = name;
            this.type = aggregator;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AbstractQueryStageAggregator that = (AbstractQueryStageAggregator) o;
            return Objects.equals(field, that.field) && Objects.equals(name, that.name) && Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(field, name, type);
        }
    }

    public static class SumAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "sum";

        @JsonCreator
        public SumAggregator(@JsonProperty("name") @NotNull String name,
                             @JsonProperty("field") String field) {
            super(field, name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class CountAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "count";

        @JsonCreator
        public CountAggregator(@JsonProperty("name") @NotNull String name,
                               @JsonProperty("field") String field) {
            super(field, name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class AvgAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "avg";

        @JsonCreator
        public AvgAggregator(@JsonProperty("name") String name,
                             @JsonProperty("field") String field) {
            super(field, name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class MaxAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "max";

        @JsonCreator
        public MaxAggregator(@JsonProperty("name") String name,
                             @JsonProperty("field") String field) {
            super(field, name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class MinAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "min";

        @JsonCreator
        public MinAggregator(@JsonProperty("name") String name,
                             @JsonProperty("field") String field) {
            super(field, name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class FirstAggregator extends AbstractQueryStageAggregator {
        public static final String TYPE = "first";

        @JsonCreator
        public FirstAggregator(@JsonProperty("name") String name,
                               @JsonProperty("field") String field) {
            super(field, name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class LastAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "last";

        @JsonCreator
        public LastAggregator(@JsonProperty("name") String name,
                              @JsonProperty("field") String field) {
            super(field, name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class RateAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "rate";

        @JsonCreator
        public RateAggregator(@JsonProperty("name") String name,
                              @JsonProperty("field") String field) {
            super(field, name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class GroupConcatAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "groupConcat";

        @JsonCreator
        public GroupConcatAggregator(@JsonProperty("name") @NotNull String name,
                                     @JsonProperty("field") @NotNull String field) {
            super(field, name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }
}
