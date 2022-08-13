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
import java.util.Objects;

/**
 * count(distinct )
 *
 * @author Frank Chen
 * @date 1/11/21 2:37 pm
 */

public class QueryStageAggregators {
    public static class CardinalityAggregator implements IQueryStageAggregator {
        public static final String TYPE = "cardinality";
        private final String name;
        private final String dimension;

        @JsonCreator
        public CardinalityAggregator(@JsonProperty("name") @NotNull String name,
                                     @JsonProperty("dimension") @NotNull String dimension) {
            this.name = name;
            this.dimension = dimension;
        }

        public String getName() {
            return name;
        }

        public String getDimension() {
            return dimension;
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CardinalityAggregator that = (CardinalityAggregator) o;
            return Objects.equals(name, that.name) && Objects.equals(dimension, that.dimension);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, dimension);
        }
    }

    abstract static class AbstractQueryStageAggregator implements IQueryStageAggregator {

        // metric name and final output name
        @Getter
        protected final String name;

        /**
         * aggregator type
         */
        @Getter
        protected final String type;

        public AbstractQueryStageAggregator(String name, String aggregator) {
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
            return Objects.equals(name, that.name) && Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, type);
        }
    }

    public static class SumAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "sum";

        @JsonCreator
        public SumAggregator(@JsonProperty("name") @NotNull String name) {
            super(name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class CountAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "count";

        @JsonCreator
        public CountAggregator(@JsonProperty("name") @NotNull String name) {
            super(name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class AvgAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "avg";

        @JsonCreator
        public AvgAggregator(@JsonProperty("name") @NotNull String name) {
            super(name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class MaxAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "max";

        @JsonCreator
        public MaxAggregator(@JsonProperty("name") @NotNull String name) {
            super(name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class MinAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "min";

        @JsonCreator
        public MinAggregator(@JsonProperty("name") @NotNull String name) {
            super(name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class FirstAggregator extends AbstractQueryStageAggregator {
        public static final String TYPE = "first";

        public FirstAggregator(@JsonProperty("name") @NotNull String name) {
            super(name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class LastAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "last";

        @JsonCreator
        public LastAggregator(@JsonProperty("name") @NotNull String name) {
            super(name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class RateAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "rate";

        @JsonCreator
        public RateAggregator(@JsonProperty("name") @NotNull String name) {
            super(name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class GroupConcatAggregator implements IQueryStageAggregator {

        public static final String TYPE = "groupConcat";
        @Getter
        private final String name;

        @Getter
        private final String field;

        @JsonCreator
        public GroupConcatAggregator(@JsonProperty("name") @NotNull String name,
                                     @JsonProperty("field") @NotNull String field) {
            this.name = Preconditions.checkArgumentNotNull("name", name);
            this.field = Preconditions.checkArgumentNotNull("field", field);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }
}
