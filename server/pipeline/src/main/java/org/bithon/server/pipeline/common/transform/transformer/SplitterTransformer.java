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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.InputRowAccessorFactory;

import java.util.function.Function;

/**
 * Deprecated. Use {@link SplitTransformer} instead
 *
 * @author frank.chen021@outlook.com
 * @date 11/4/22 11:52 PM
 */
@Deprecated
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

    @JsonIgnore

    private final Function<IInputRow, Object> getValue;

    @JsonCreator
    public SplitterTransformer(@JsonProperty("field") String field,
                               @JsonProperty("splitter") String splitter,
                               @JsonProperty("names") String... names) {
        this.field = Preconditions.checkArgumentNotNull("field", field);
        this.splitter = Preconditions.checkArgumentNotNull("splitter", splitter);
        this.names = names;
        this.getValue = InputRowAccessorFactory.createGetter(this.field);
    }

    @Override
    public TransformResult transform(IInputRow row) {
        Object val = getValue.apply(row);
        if (val == null) {
            return TransformResult.CONTINUE;
        }

        String[] values = val.toString().split(splitter);
        for (int i = 0, len = Math.min(names.length, values.length); i < len; i++) {
            row.updateColumn(names[i], values[i]);
        }
        return TransformResult.CONTINUE;
    }
}
