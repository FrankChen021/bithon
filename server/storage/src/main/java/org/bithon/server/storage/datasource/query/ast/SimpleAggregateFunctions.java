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

package org.bithon.server.storage.datasource.query.ast;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.utils.StringUtils;

/**
 * @author Frank Chen
 * @date 1/11/21 2:37 pm
 */

public class SimpleAggregateFunctions {

    public static SimpleAggregateFunction create(String type, String field) {
        String json = StringUtils.format("{\"type\": \"%s\", \"field\": \"%s\"}", type, field);
        try {
            return new ObjectMapper().readValue(json, SimpleAggregateFunction.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    abstract static class AbstractQueryStageAggregateFunction extends SimpleAggregateFunction {
        public AbstractQueryStageAggregateFunction(String field, String aggregator) {
            super(aggregator);
            getArguments().add(new Name(field));
        }
    }

    public static class CardinalityAggregateFunction extends AbstractQueryStageAggregateFunction {
        public static final String TYPE = "cardinality";

        @JsonCreator
        public CardinalityAggregateFunction(@JsonProperty("field") String field) {
            super(field, TYPE);
        }

        @Override
        public <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class SumAggregateFunction extends AbstractQueryStageAggregateFunction {

        public static final String TYPE = "sum";

        @JsonCreator
        public SumAggregateFunction(@JsonProperty("field") String field) {
            super(field, TYPE);
        }

        @Override
        public <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class CountAggregateFunction extends AbstractQueryStageAggregateFunction {

        public static final String TYPE = "count";

        @JsonCreator
        public CountAggregateFunction(@JsonProperty("field") String field) {
            super(field, TYPE);
        }

        @Override
        public <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class AvgAggregateFunction extends AbstractQueryStageAggregateFunction {

        public static final String TYPE = "avg";

        @JsonCreator
        public AvgAggregateFunction(@JsonProperty("field") String field) {
            super(field, TYPE);
        }

        @Override
        public <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class MaxAggregateFunction extends AbstractQueryStageAggregateFunction {

        public static final String TYPE = "max";

        @JsonCreator
        public MaxAggregateFunction(@JsonProperty("field") String field) {
            super(field, TYPE);
        }

        @Override
        public <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class MinAggregateFunction extends AbstractQueryStageAggregateFunction {

        public static final String TYPE = "min";

        @JsonCreator
        public MinAggregateFunction(@JsonProperty("field") String field) {
            super(field, TYPE);
        }

        @Override
        public <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class FirstAggregateFunction extends AbstractQueryStageAggregateFunction {
        public static final String TYPE = "first";

        @JsonCreator
        public FirstAggregateFunction(@JsonProperty("field") String field) {
            super(field, TYPE);
        }

        @Override
        public <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class LastAggregateFunction extends AbstractQueryStageAggregateFunction {

        public static final String TYPE = "last";

        @JsonCreator
        public LastAggregateFunction(@JsonProperty("field") String field) {
            super(field, TYPE);
        }

        @Override
        public <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class RateAggregateFunction extends AbstractQueryStageAggregateFunction {

        public static final String TYPE = "rate";

        @JsonCreator
        public RateAggregateFunction(@JsonProperty("field") String field) {
            super(field, TYPE);
        }

        @Override
        public <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class GroupConcatAggregateFunction extends AbstractQueryStageAggregateFunction {

        public static final String TYPE = "groupConcat";

        @JsonCreator
        public GroupConcatAggregateFunction(@JsonProperty("field") String field) {
            super(field, TYPE);
        }

        @Override
        public <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }
}
