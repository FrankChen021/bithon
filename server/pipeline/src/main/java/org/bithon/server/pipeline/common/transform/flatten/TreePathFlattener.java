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

package org.bithon.server.pipeline.common.transform.flatten;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.PathExpression;


/**
 * @author frank.chen021@outlook.com
 * @date 11/4/22 11:17 PM
 */
public class TreePathFlattener implements IFlattener {

    /**
     * the field name that holds the flattened object
     */
    @Getter
    private final String field;

    @Getter
    private final String path;
    private final PathExpression pathExpression;

    @JsonCreator
    public TreePathFlattener(@JsonProperty("field") String field,
                             @JsonProperty("path") String path) {
        this.field = Preconditions.checkArgumentNotNull("field", field);
        this.path = Preconditions.checkArgumentNotNull("path", path);
        this.pathExpression = PathExpression.Builder.build(this.path);
    }

    @Override
    public void flatten(IInputRow inputRow) {
        Object obj = pathExpression.evaluate(inputRow);
        if (obj != null) {
            inputRow.updateColumn(field, obj);
        }
    }
}
