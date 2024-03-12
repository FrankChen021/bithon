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
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.storage.datasource.input.IInputRow;

import java.util.Map;
import java.util.function.Function;

/**
 * Can be seen as:
 *  resultField = testField is null ? trueValue : falseValue
 *
 * @author frank.chen021@outlook.com
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

    private final Function<IInputRow, Object> valueExtractor;

    @JsonCreator
    public HasFieldTransformer(@JsonProperty("testField") String testField,
                               @JsonProperty("resultField") String resultField,
                               @JsonProperty("trueValue") Object trueValue,
                               @JsonProperty("falseValue") Object falseValue) {
        this.testField = Preconditions.checkArgumentNotNull("testField", testField);
        this.resultField = Preconditions.checkArgumentNotNull("resultField", resultField);
        this.trueValue = trueValue;
        this.falseValue = falseValue;

        int dotSeparatorIndex = this.testField.indexOf('.');
        if (dotSeparatorIndex >= 0) {
            final String container = this.testField.substring(0, dotSeparatorIndex);
            final String nested = this.testField.substring(dotSeparatorIndex + 1);
            valueExtractor = inputRow -> {
                Object v = inputRow.getCol(container);
                if (v instanceof Map) {
                    return ((Map<?, ?>) v).get(nested);
                }
                return null;
            };
        } else {
            valueExtractor = inputRow -> inputRow.getCol(this.testField);
        }
    }

    @Override
    public boolean transform(IInputRow inputRow) {
        Object v = valueExtractor.apply(inputRow) != null ? trueValue : falseValue;
        inputRow.updateColumn(resultField, v);
        return true;
    }
}
