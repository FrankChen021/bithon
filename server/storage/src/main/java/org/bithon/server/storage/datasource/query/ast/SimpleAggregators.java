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
import org.bithon.server.storage.datasource.query.IQueryStageAggregatorVisitor;

/**
 * @author Frank Chen
 * @date 1/11/21 2:37 pm
 */

public class SimpleAggregators {

    public static SimpleAggregator create(String type, String field) {
        String json = StringUtils.format("{\"type\": \"%s\", \"field\": \"%s\"}", type, field);
        try {
            return new ObjectMapper().readValue(json, SimpleAggregator.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    abstract static class AbstractQueryStageAggregator extends SimpleAggregator {
        public AbstractQueryStageAggregator(String field, String aggregator) {
            super(aggregator);
            getArguments().add(new Name(field));
        }
    }

    public static class CardinalityAggregator extends AbstractQueryStageAggregator {
        public static final String TYPE = "cardinality";

        @JsonCreator
        public CardinalityAggregator(@JsonProperty("field") String field) {
            super(field, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class SumAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "sum";

        @JsonCreator
        public SumAggregator(@JsonProperty("field") String field) {
            super(field, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class CountAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "count";

        @JsonCreator
        public CountAggregator(@JsonProperty("field") String field) {
            super(field, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class AvgAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "avg";

        @JsonCreator
        public AvgAggregator(@JsonProperty("field") String field) {
            super(field, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class MaxAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "max";

        @JsonCreator
        public MaxAggregator(@JsonProperty("field") String field) {
            super(field, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class MinAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "min";

        @JsonCreator
        public MinAggregator(@JsonProperty("field") String field) {
            super(field, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class FirstAggregator extends AbstractQueryStageAggregator {
        public static final String TYPE = "first";

        @JsonCreator
        public FirstAggregator(@JsonProperty("field") String field) {
            super(field, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class LastAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "last";

        @JsonCreator
        public LastAggregator(@JsonProperty("field") String field) {
            super(field, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class RateAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "rate";

        @JsonCreator
        public RateAggregator(@JsonProperty("field") String field) {
            super(field, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class GroupConcatAggregator extends AbstractQueryStageAggregator {

        public static final String TYPE = "groupConcat";

        @JsonCreator
        public GroupConcatAggregator(@JsonProperty("field") String field) {
            super(field, TYPE);
        }

        @Override
        public <T> T accept(IQueryStageAggregatorVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }
}
