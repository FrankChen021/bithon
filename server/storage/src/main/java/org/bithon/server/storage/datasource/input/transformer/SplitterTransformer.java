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
import org.bithon.server.storage.datasource.input.InputRowAccessorFactory;

import java.util.function.Function;

/**
 * @author frank.chen021@outlook.com
 * @date 11/4/22 11:52 PM
 */
public class SplitterTransformer implements ITransformer {

    /**
     * source field
     */
    @Getter
    private final String field;

    @Getter
    private final String splitter;

    @Getter
    private final String[] names;

    private final Function<IInputRow, Object> valueExtractor;

    @JsonCreator
    public SplitterTransformer(@JsonProperty("field") String field,
                               @JsonProperty("splitter") String splitter,
                               @JsonProperty("names") String... names) {
        this.field = Preconditions.checkArgumentNotNull("field", field);
        this.splitter = Preconditions.checkArgumentNotNull("splitter", splitter);
        this.names = names;
        this.valueExtractor = InputRowAccessorFactory.createGetter(this.field);
    }

    @Override
    public void transform(IInputRow row) {
        Object val = valueExtractor.apply(row);
        if (val == null) {
            return;
        }

        String[] values = val.toString().split(splitter);
        for (int i = 0, len = Math.min(names.length, values.length); i < len; i++) {
            row.updateColumn(names[i], values[i]);
        }
    }
}
