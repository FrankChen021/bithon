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

package org.bithon.server.pipeline.common.transform.flatten;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.pipeline.common.transform.transformer.FlattenTransformer;
import org.bithon.server.pipeline.common.transform.transformer.ITransformer;
import org.bithon.server.storage.datasource.input.IInputRow;

/**
 * A special kind of transformer that is executed before a {@link ITransformer}
 *
 * Deprecated, use {@link FlattenTransformer} instead
 *
 * @author frank.chen021@outlook.com
 * @date 2020/12/28
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = "tree", value = TreePathFlattener.class),
})
@Deprecated
public interface IFlattener {

    /**
     * the field name generated by this flattener
     */
    String getField();

    void flatten(IInputRow inputRow);
}
