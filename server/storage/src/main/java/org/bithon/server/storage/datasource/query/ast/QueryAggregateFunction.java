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
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.utils.StringUtils;


/**
 * Aggregator for built-in types.
 *
 * @author Frank Chen
 * @date 1/11/21 2:36 pm
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = QueryAggregateFunction.Cardinality.TYPE, value = QueryAggregateFunction.Cardinality.class),
    @JsonSubTypes.Type(name = QueryAggregateFunction.Sum.TYPE, value = QueryAggregateFunction.Sum.class),
    @JsonSubTypes.Type(name = QueryAggregateFunction.Count.TYPE, value = QueryAggregateFunction.Count.class),
    @JsonSubTypes.Type(name = QueryAggregateFunction.Avg.TYPE, value = QueryAggregateFunction.Avg.class),
    @JsonSubTypes.Type(name = QueryAggregateFunction.Min.TYPE, value = QueryAggregateFunction.Min.class),
    @JsonSubTypes.Type(name = QueryAggregateFunction.Max.TYPE, value = QueryAggregateFunction.Max.class),
    @JsonSubTypes.Type(name = QueryAggregateFunction.First.TYPE, value = QueryAggregateFunction.First.class),
    @JsonSubTypes.Type(name = QueryAggregateFunction.Last.TYPE, value = QueryAggregateFunction.Last.class),
    @JsonSubTypes.Type(name = QueryAggregateFunction.Rate.TYPE, value = QueryAggregateFunction.Rate.class),
    @JsonSubTypes.Type(name = QueryAggregateFunction.GroupConcat.TYPE, value = QueryAggregateFunction.GroupConcat.class),
})
public abstract class QueryAggregateFunction extends Function {
    public QueryAggregateFunction(String fnName, String field) {
        super(fnName);
        getArguments().add(new Column(field));
    }

    /**
     * get the column that aggregation is performed on
     */
    public String getTargetColumn() {
        return ((Column) this.getArguments().get(0)).getName();
    }

    public abstract <T> T accept(IQueryAggregateFunctionVisitor<T> visitor);

    @Override
    public String toString() {
        return StringUtils.format("%s(%s)", this.getFnName(), getTargetColumn());
    }


    public static Function create(String type, String field) {
        String json = StringUtils.format("{\"type\": \"%s\", \"field\": \"%s\"}", type, field);
        try {
            return new ObjectMapper().readValue(json, QueryAggregateFunction.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Cardinality extends QueryAggregateFunction {
        public static final String TYPE = "cardinality";

        @JsonCreator
        public Cardinality(@JsonProperty("field") String field) {
            super(TYPE, field);
        }

        @Override
        public <T> T accept(IQueryAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Sum extends QueryAggregateFunction {

        public static final String TYPE = "sum";

        @JsonCreator
        public Sum(@JsonProperty("field") String field) {
            super(TYPE, field);
        }

        @Override
        public <T> T accept(IQueryAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Count extends QueryAggregateFunction {

        public static final String TYPE = "count";

        @JsonCreator
        public Count(@JsonProperty("field") String field) {
            super(TYPE, field);
        }

        @Override
        public <T> T accept(IQueryAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Avg extends QueryAggregateFunction {

        public static final String TYPE = "avg";

        @JsonCreator
        public Avg(@JsonProperty("field") String field) {
            super(TYPE, field);
        }

        @Override
        public <T> T accept(IQueryAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Max extends QueryAggregateFunction {

        public static final String TYPE = "max";

        @JsonCreator
        public Max(@JsonProperty("field") String field) {
            super(TYPE, field);
        }

        @Override
        public <T> T accept(IQueryAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Min extends QueryAggregateFunction {

        public static final String TYPE = "min";

        @JsonCreator
        public Min(@JsonProperty("field") String field) {
            super(TYPE, field);
        }

        @Override
        public <T> T accept(IQueryAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class First extends QueryAggregateFunction {
        public static final String TYPE = "first";

        @JsonCreator
        public First(@JsonProperty("field") String field) {
            super(TYPE, field);
        }

        @Override
        public <T> T accept(IQueryAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Last extends QueryAggregateFunction {

        public static final String TYPE = "last";

        @JsonCreator
        public Last(@JsonProperty("field") String field) {
            super(TYPE, field);
        }

        @Override
        public <T> T accept(IQueryAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class Rate extends QueryAggregateFunction {

        public static final String TYPE = "rate";

        @JsonCreator
        public Rate(@JsonProperty("field") String field) {
            super(TYPE, field);
        }

        @Override
        public <T> T accept(IQueryAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    public static class GroupConcat extends QueryAggregateFunction {

        public static final String TYPE = "groupConcat";

        @JsonCreator
        public GroupConcat(@JsonProperty("field") String field) {
            super(TYPE, field);
        }

        @Override
        public <T> T accept(IQueryAggregateFunctionVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }
}
