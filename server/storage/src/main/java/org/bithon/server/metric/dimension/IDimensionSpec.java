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

package org.bithon.server.metric.dimension;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.metric.dimension.transformer.IDimensionTransformer;
import org.bithon.server.metric.typing.IValueType;

/**
 * @author frankchen
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = StringDimensionSpec.class)
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = "long", value = LongDimensionSpec.class),
    @JsonSubTypes.Type(name = "string", value = StringDimensionSpec.class)
})
public interface IDimensionSpec {

    String getAlias();

    String getName();

    String getDisplayText();

    boolean isRequired();

    int getLength();

    /**
     * 对用户是否可见
     */
    boolean isVisible();

    IDimensionTransformer getTransformer();

    @JsonIgnore
    IValueType getValueType();

    <T> T accept(IDimensionSpecVisitor<T> visitor);
}
