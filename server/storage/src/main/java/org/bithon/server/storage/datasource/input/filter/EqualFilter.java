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

package org.bithon.server.storage.datasource.input.filter;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.server.storage.datasource.input.IInputRow;

import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/24
 */
public class EqualFilter implements IInputRowFilter {

    @Getter
    @NotNull
    private final String field;

    @Getter
    @NotNull
    private final Object value;

    @JsonCreator
    public EqualFilter(@JsonProperty("field") String field,
                       @JsonProperty("value") @NotNull Object value) {
        this.field = field;
        this.value = value;
    }

    @Override
    public boolean shouldInclude(IInputRow inputRow) {
        return value.equals(inputRow.getCol(this.field));
    }
}
