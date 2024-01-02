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

package org.bithon.server.storage.datasource.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import org.bithon.server.commons.matcher.EqualMatcher;
import org.bithon.server.commons.matcher.GreaterThanMatcher;
import org.bithon.server.commons.matcher.GreaterThanOrEqualMatcher;
import org.bithon.server.commons.matcher.IMatcher;
import org.bithon.server.commons.matcher.LessThanMatcher;
import org.bithon.server.commons.matcher.LessThanOrEqualMatcher;
import org.bithon.server.commons.matcher.NotEqualMatcher;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/30 4:43 下午
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "predicate")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = ">", value = IColumnFilter.GreaterThanFilter.class),
    @JsonSubTypes.Type(name = ">=", value = IColumnFilter.GreaterThanOrEqualFilter.class),
    @JsonSubTypes.Type(name = "<", value = IColumnFilter.LessThanFilter.class),
    @JsonSubTypes.Type(name = "<=", value = IColumnFilter.LessThanOrEqualFilter.class),
    @JsonSubTypes.Type(name = "<>", value = IColumnFilter.NotEqualFilter.class),
    @JsonSubTypes.Type(name = "=", value = IColumnFilter.EqualFilter.class),
})
public interface IColumnFilter {
    String getField();

    String getPredicate();

    Object getExpected();

    @JsonIgnore
    IMatcher getMatcher();

    <T> T accept(IColumnFilterVisitor<T> visitor);

    @Getter
    abstract class AbstractFilter implements IColumnFilter {
        protected String field;
        protected String predicate;
        protected Object expected;
        protected IMatcher matcher;

        public AbstractFilter(String field,
                              String predicate,
                              Object expected,
                              IMatcher matcher) {
            this.field = field;
            this.predicate = predicate;
            this.expected = expected;
            this.matcher = matcher;
        }
    }

    class GreaterThanFilter extends AbstractFilter {
        @JsonCreator
        public GreaterThanFilter(@JsonProperty("field") String field,
                                 @JsonProperty("expected") Object expected) {
            super(field, ">", expected, new GreaterThanMatcher(expected));
        }

        @Override
        public <T> T accept(IColumnFilterVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    class GreaterThanOrEqualFilter extends AbstractFilter {
        @JsonCreator
        public GreaterThanOrEqualFilter(@JsonProperty("field") String field,
                                        @JsonProperty("expected") Object expected) {
            super(field, ">=", expected, new GreaterThanOrEqualMatcher(expected));
        }

        @Override
        public <T> T accept(IColumnFilterVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    class EqualFilter extends AbstractFilter {
        @JsonCreator
        public EqualFilter(@JsonProperty("field") String field,
                           @JsonProperty("expected") Object expected) {
            super(field, "=", expected, new EqualMatcher(expected));
        }

        @Override
        public <T> T accept(IColumnFilterVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    class LessThanFilter extends AbstractFilter {
        @JsonCreator
        public LessThanFilter(@JsonProperty("field") String field,
                              @JsonProperty("expected") Object expected) {
            super(field, "<", expected, new LessThanMatcher(expected));
        }

        @Override
        public <T> T accept(IColumnFilterVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    class LessThanOrEqualFilter extends AbstractFilter {
        @JsonCreator
        public LessThanOrEqualFilter(@JsonProperty("field") String field,
                                     @JsonProperty("expected") Object expected) {
            super(field, "<=", expected, new LessThanOrEqualMatcher(expected));
        }

        @Override
        public <T> T accept(IColumnFilterVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }

    class NotEqualFilter extends AbstractFilter {
        @JsonCreator
        public NotEqualFilter(@JsonProperty("field") String field,
                              @JsonProperty("expected") Object expected) {
            super(field, "<>", expected, new NotEqualMatcher(expected));
        }

        @Override
        public <T> T accept(IColumnFilterVisitor<T> visitor) {
            return visitor.visit(this);
        }
    }
}
