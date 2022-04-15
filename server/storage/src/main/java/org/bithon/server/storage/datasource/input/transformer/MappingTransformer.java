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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.server.storage.datasource.input.IInputRow;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * A transformer that maps a value of a field to another.
 *
 * @author Frank Chen
 */
public class MappingTransformer extends AbstractSimpleTransformer {

    @Getter
    private final Map<String, Object> maps;

    public MappingTransformer(@JsonProperty("field") String field,
                              @JsonProperty("maps") @NotNull Map<String, Object> maps) {
        super(field);
        this.maps = maps;
    }

    @Override
    protected Object transformInternal(IInputRow row) {
        if (row == null) {
            return null;
        }
        String val = row.getColAsString(field);
        return val == null ? null : maps.getOrDefault(val, val);
    }
}
