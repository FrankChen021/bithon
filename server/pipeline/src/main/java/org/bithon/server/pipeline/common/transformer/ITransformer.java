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

package org.bithon.server.pipeline.common.transformer;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.datasource.column.IColumn;
import org.bithon.server.datasource.column.aggregatable.IAggregatableColumn;
import org.bithon.server.datasource.input.IInputRow;

/**
 * NOTE: for implementations, SHOULD use {@link AbstractTransformer}
 * <p>
 * A transformer allows adding new fields to input rows.
 * Each one has a "name" (the name of the new field) which can be referred to by {@link IColumn},
 * or {@link IAggregatableColumn}.
 * <p>
 * The transformer produces values for this new field based on looking at the entire input row.
 *
 * @author frank.chen021@outlook.com
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = "flatten", value = FlattenTransformer.class),

    // Text processing
    @JsonSubTypes.Type(name = "regexpr", value = RegExprTransformer.class),
    @JsonSubTypes.Type(name = "replace", value = ReplaceTransformer.class),
    @JsonSubTypes.Type(name = "split", value = SplitTransformer.class),

    // Drop
    @JsonSubTypes.Type(name = "drop", value = DropTransformer.class),
    @JsonSubTypes.Type(name = "probabilistic_sampler", value = ProbabilisticSamplerTransform.class),

    // Generic processing
    @JsonSubTypes.Type(name = "expression", value = ExpressionTransformer.class),
})
public interface ITransformer {

    class TransformException extends RuntimeException {

        public TransformException(String message) {
            super(message);
        }
    }

    TransformResult transform(IInputRow inputRow) throws TransformException;
}
