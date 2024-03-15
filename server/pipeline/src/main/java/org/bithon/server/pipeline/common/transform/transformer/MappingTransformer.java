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

package org.bithon.server.pipeline.common.transform.transformer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.server.storage.datasource.input.IInputRow;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * A transformer that maps the value of a field to another.
 * For example:
 * {
 * "field": "a",
 * "maps": {
 * "b": "c"
 * }
 * }
 * if the value of "a" is b, then its value will be changed to c.
 * <p>
 * Can be seen as the below expression:
 * a == 'b' ? 'c' : 'b'
 * <p>
 * Use {@link ExpressionTransformer} instead
 *
 * @author frank.chen021@outlook.com
 */
@Deprecated
public class MappingTransformer implements ITransformer {

    @Getter
    private final String field;

    @Getter
    private final Map<String, Object> maps;

    public MappingTransformer(@JsonProperty("field") String field,
                              @JsonProperty("maps") @NotNull Map<String, Object> maps) {
        this.field = field;
        this.maps = maps;
    }

    @Override
    public TransformResult transform(IInputRow row) {
        if (row == null) {
            return TransformResult.DROP;
        }

        String val = row.getColAsString(field);
        Object v = val == null ? null : maps.getOrDefault(val, val);
        if (v != null) {
            row.updateColumn(field, v);
        }

        return TransformResult.CONTINUE;
    }
}
