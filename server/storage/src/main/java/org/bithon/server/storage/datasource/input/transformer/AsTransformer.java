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
 * @author frank.chen021@outlook.com
 * @date 2023/6/26 22:13
 */
public class AsTransformer implements ITransformer {

    @Getter
    private final String name;

    @Getter
    private final String as;

    @JsonCreator
    public AsTransformer(@JsonProperty("name") String name,
                         @JsonProperty("as") String as) {
        this.name = name;
        this.as = as;
    }

    @Override
    public boolean transform(IInputRow inputRow) throws TransformException {
        Object val = inputRow.getCol(name);
        if (val != null) {
            inputRow.updateColumn(as, val);
        }
        return true;
    }
}
