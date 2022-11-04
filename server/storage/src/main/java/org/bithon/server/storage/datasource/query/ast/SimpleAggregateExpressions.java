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

public class SimpleAggregateExpressions {

    public static SimpleAggregateExpression create(String type, String field) {
        String json = StringUtils.format("{\"type\": \"%s\", \"field\": \"%s\"}", type, field);
        try {
            return new ObjectMapper().readValue(json, SimpleAggregateExpression.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class CardinalityAggregateExpression extends SimpleAggregateExpression {
        public static final String TYPE = "cardinality";

        @JsonCreator
        public CardinalityAggregateExpression(@JsonProperty("field") String field) {
            super(TYPE, field);
        }

        @Override
        public <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class SumAggregateExpression extends SimpleAggregateExpression {

        public static final String TYPE = "sum";

        @JsonCreator
        public SumAggregateExpression(@JsonProperty("field") String field) {
            super(TYPE, field);
        }

        @Override
        public <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class CountAggregateExpression extends SimpleAggregateExpression {

        public static final String TYPE = "count";

        @JsonCreator
        public CountAggregateExpression(@JsonProperty("field") String field) {
            super(TYPE, field);
        }

        @Override
        public <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class AvgAggregateExpression extends SimpleAggregateExpression {

        public static final String TYPE = "avg";

        @JsonCreator
        public AvgAggregateExpression(@JsonProperty("field") String field) {
            super(TYPE, field);
        }

        @Override
        public <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class MaxAggregateExpression extends SimpleAggregateExpression {

        public static final String TYPE = "max";

        @JsonCreator
        public MaxAggregateExpression(@JsonProperty("field") String field) {
            super(TYPE, field);
        }

        @Override
        public <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class MinAggregateExpression extends SimpleAggregateExpression {

        public static final String TYPE = "min";

        @JsonCreator
        public MinAggregateExpression(@JsonProperty("field") String field) {
            super(TYPE, field);
        }

        @Override
        public <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class FirstAggregateExpression extends SimpleAggregateExpression {
        public static final String TYPE = "first";

        @JsonCreator
        public FirstAggregateExpression(@JsonProperty("field") String field) {
            super(TYPE, field);
        }

        @Override
        public <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class LastAggregateExpression extends SimpleAggregateExpression {

        public static final String TYPE = "last";

        @JsonCreator
        public LastAggregateExpression(@JsonProperty("field") String field) {
            super(TYPE, field);
        }

        @Override
        public <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class RateAggregateExpression extends SimpleAggregateExpression {

        public static final String TYPE = "rate";

        @JsonCreator
        public RateAggregateExpression(@JsonProperty("field") String field) {
            super(TYPE, field);
        }

        @Override
        public <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class GroupConcatAggregateExpression extends SimpleAggregateExpression {

        public static final String TYPE = "groupConcat";

        @JsonCreator
        public GroupConcatAggregateExpression(@JsonProperty("field") String field) {
            super(TYPE, field);
        }

        @Override
        public <T> T accept(ISimpleAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }
}
