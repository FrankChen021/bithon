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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.server.storage.datasource.input.IInputRow;

import java.util.Map;

/**
 * @author Frank Chen
 * @date 11/4/22 11:17 PM
 */
public class TreePathFlattener implements IFlattener {

    /**
     * the field name that holds the flattened object
     */
    @Getter
    private final String name;

    /**
     * dot separated path such as a.b.c
     */
    @Getter
    private final String path;

    /**
     * runtime property that holds the splitted {@link #path}
     */
    @JsonIgnore
    @Getter
    private final String[] nodes;

    @JsonCreator
    public TreePathFlattener(@JsonProperty("name") String name,
                             @JsonProperty("path") String path) {
        this.name = name;
        this.path = path;
        this.nodes = path.split("\\.");
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void flatten(IInputRow inputRow) {
        Object obj = inputRow.getCol(nodes[0]);
        if (!(obj instanceof Map)) {
            // error
            return;
        }

        if (nodes.length > 1) {
            Map map = (Map) obj;
            for (int i = 1; i < nodes.length - 1; i++) {
                obj = map.get(nodes[i]);
                if (!(obj instanceof Map)) {
                    return;
                }
                map = (Map) obj;
            }

            obj = map.get(nodes[nodes.length - 1]);
        }

        if (obj != null) {
            inputRow.updateColumn(name, obj);
        }
    }
}
