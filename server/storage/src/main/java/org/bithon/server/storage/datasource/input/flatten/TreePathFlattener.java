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

package org.bithon.server.storage.datasource.input.flatten;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.ReflectionUtils;
import org.bithon.server.storage.datasource.input.IInputRow;

import java.util.Map;

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
    private final String[] nodes;

    /**
     * For test only
     */
    public TreePathFlattener(String field, String path) {
        this(field, path, null);
    }

    @JsonCreator
    public TreePathFlattener(@JsonProperty("field") String field,
                             @JsonProperty("path") String path,
                             @JsonProperty("nodes") String[] nodes) {
        this.field = field;
        this.nodes = path == null ? nodes : path.split("\\.");
        Preconditions.checkIfTrue(this.nodes != null && this.nodes.length > 0, "nodes can't be empty");
    }

    @Override
    public void flatten(IInputRow inputRow) {
        Object obj = inputRow.getCol(nodes[0]);
        if (obj == null) {
            return;
        }

        if (nodes.length > 1) {
            for (int i = 1; i < nodes.length - 1; i++) {
                String prop = nodes[i];
                obj = getValue(obj, prop);
                if (obj == null) {
                    return;
                }
            }

            obj = getValue(obj, nodes[nodes.length - 1]);
        }

        if (obj != null) {
            inputRow.updateColumn(field, obj);
        }
    }

    private Object getValue(Object v, String property) {
        if (v instanceof Map) {
            return ((Map<?, ?>) v).get(property);
        } else {
            return ReflectionUtils.getFieldValue(v, property);
        }
    }
}
