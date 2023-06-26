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

package org.bithon.server.storage.datasource.input.transformer;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.spec.IMetricSpec;

/**
 * A transformer allows adding new fields to input rows.
 * Each one has a "name" (the name of the new field) which can be referred to by {@link org.bithon.server.storage.datasource.dimension.IDimensionSpec},
 * or {@link IMetricSpec}.
 * <p>
 * The transformer produces values for this new field based on looking at the entire input row.
 *
 * @author frank.chen021@outlook.com
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = "mapping", value = MappingTransformer.class),
    @JsonSubTypes.Type(name = "splitter", value = SplitterTransformer.class),
    @JsonSubTypes.Type(name = "chain", value = ChainTransformer.class),
    @JsonSubTypes.Type(name = "add", value = AddFieldTransformer.class),
    @JsonSubTypes.Type(name = "has", value = HasFieldTransformer.class),
        @JsonSubTypes.Type(name = "as", value = AsTransformer.class),
    @JsonSubTypes.Type(name = "regexpr", value = RegExprTransformer.class)
})
public interface ITransformer {

    class TransformException extends RuntimeException {
    }

    void transform(IInputRow inputRow) throws TransformException;
}
