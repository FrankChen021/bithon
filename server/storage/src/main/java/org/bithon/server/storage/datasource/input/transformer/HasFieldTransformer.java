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
 * @author Frank Chen
 * @date 14/4/22 5:22 PM
 */
public class HasFieldTransformer implements ITransformer {

    @Getter
    private final String testField;

    @Getter
    private final String resultField;

    @Getter
    private final Object trueValue;

    @Getter
    private final Object falseValue;

    @JsonCreator
    public HasFieldTransformer(@JsonProperty("testField") String testField,
                               @JsonProperty("resultField") String resultField,
                               @JsonProperty("trueValue") Object trueValue,
                               @JsonProperty("falseValue") Object falseValue) {
        this.testField = testField;
        this.resultField = resultField;
        this.trueValue = trueValue;
        this.falseValue = falseValue;
    }

    @Override
    public void transform(IInputRow inputRow) {
        Object v = inputRow.getCol(testField) != null ? trueValue : falseValue;
        inputRow.updateColumn(resultField, v);
    }
}
