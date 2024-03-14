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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.server.storage.datasource.input.IInputRow;

/**
 * Use {@link ExpressionTransformer} instead
 *
 * @author frank.chen021@outlook.com
 * @date 13/4/22 4:57 PM
 */
@Deprecated
public class AddFieldTransformer extends AbstractTransformer {

    @Getter
    private final String field;

    @Getter
    private final String value;

    @JsonCreator
    public AddFieldTransformer(@JsonProperty("field") String field,
                               @JsonProperty("value") String value,
                               @JsonProperty("where") String where) {
        super(where);
        this.field = field;
        this.value = value;
    }

    @Override
    public TransformResult transformInternal(IInputRow inputRow) {
        if (field != null && value != null) {
            inputRow.updateColumn(field, value);
        }
        return TransformResult.CONTINUE;
    }
}
