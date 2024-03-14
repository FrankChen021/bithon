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

/**
 * @author frank.chen021@outlook.com
 * @date 2023/6/26 22:13
 */
public class FlattenTransformer extends AbstractTransformer {

    @Getter
    private final String[] sources;

    @Getter
    private final String[] targets;

    @JsonCreator
    public FlattenTransformer(@JsonProperty("sources") String[] sources,
                              @JsonProperty("targets") String[] targets,
                              @JsonProperty("where") String where) {
        super(where);

        Preconditions.checkNotNull(sources, "sources can't be null");
        Preconditions.checkNotNull(targets, "sources can't be null");
        Preconditions.checkIfTrue(sources.length == targets.length, "The length of sources and targets is not the same");
        this.sources = sources;
        this.targets = targets;
    }

    @Override
    protected TransformResult transformInternal(IInputRow inputRow) throws TransformException {
        for (int i = 0; i < sources.length; i++) {
            Object val = inputRow.getCol(sources[i]);
            if (val != null) {
                inputRow.updateColumn(targets[i], val);
            }
        }

        return TransformResult.CONTINUE;
    }
}
