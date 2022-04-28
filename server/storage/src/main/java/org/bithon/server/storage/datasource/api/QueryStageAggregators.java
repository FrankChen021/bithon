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

import javax.validation.constraints.NotNull;

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
    }

    public static class SumAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "sum";

        public SumAggregator(String name) {
            super(name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class CountAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "count";

        public CountAggregator(String name) {
            super(name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class AvgAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "avg";

        public AvgAggregator(String name) {
            super(name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class MaxAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "max";

        public MaxAggregator(String name) {
            super(name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class MinAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "min";

        public MinAggregator(String name) {
            super(name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class FirstAggregator extends AbstractQueryStageAggregator {
        public static final String TYPE = "first";

        public FirstAggregator(String name) {
            super(name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class LastAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "last";

        public LastAggregator(String name) {
            super(name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class RateAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "rate";

        public RateAggregator(String name) {
            super(name, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }
}
