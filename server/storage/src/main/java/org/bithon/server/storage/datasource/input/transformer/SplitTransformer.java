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
import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class SplitTransformer extends AbstractTransformer {

    @Getter
    private final String source;

    @Getter
    private final String by;

    @Getter
    private final String[] targets;

    @JsonIgnore
    private final Function<IInputRow, Object> getValue;

    @JsonCreator
    public SplitTransformer(@JsonProperty("source") String source,
                            @JsonProperty("splitter") String by,
                            @JsonProperty("targets") String[] targets,
                            @JsonProperty("where") String where) {
        super(where);

        this.source = Preconditions.checkArgumentNotNull("source", source);
        this.by = Preconditions.checkArgumentNotNull("by", by);
        this.targets = Preconditions.checkArgumentNotNull("targets", targets);

        this.getValue = InputRowAccessorFactory.createGetter(this.source);
    }

    @Override
    protected TransformResult transformInternal(IInputRow row) {
        Object val = getValue.apply(row);
        if (!(val instanceof String)) {
            return TransformResult.CONTINUE;
        }

        String[] values = val.toString().split(by);
        for (int i = 0, len = Math.min(targets.length, values.length); i < len; i++) {
            row.updateColumn(targets[i], values[i]);
        }
        return TransformResult.CONTINUE;
    }
}
