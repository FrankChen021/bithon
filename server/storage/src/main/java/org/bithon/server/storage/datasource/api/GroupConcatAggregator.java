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
 * @author Frank Chen
 * @date 5/8/22 4:56 PM
 */
public class GroupConcatAggregator implements IQueryableAggregator {

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
    public <T> T accept(IQueryableAggregatorVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
