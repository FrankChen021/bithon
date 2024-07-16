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

package org.bithon.server.storage.datasource.input;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/2 4:46 下午
 */
public class InputRow implements IInputRow {
    @Getter
    private final Map<String, Object> columns;

    @Getter
    private final JsonNode rootNode;

    public InputRow(final ObjectMapper mapper, final JsonNode rootNode) {
        this.columns = mapper.convertValue(rootNode, new TypeReference<Map<String, Object>>() {
        });
        this.rootNode = rootNode;
    }

    public InputRow(final Map<String, Object> map) {
        this.columns = map;
        this.rootNode = null;
    }

    public InputRow(Object obj) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        this.rootNode = mapper.readValue(mapper.writeValueAsBytes(obj), JsonNode.class);
        this.columns = mapper.convertValue(rootNode, new TypeReference<Map<String, Object>>() {
        });
    }

    @Override
    public Object getCol(String columnName) {
        return columns.get(columnName);
    }

    @Override
    public void updateColumn(String name, Object value) {
        columns.put(name, value);
    }

    @Override
    public Map<String, Object> toMap() {
        return new HashMap<>(columns);
    }

    @Override
    public String toString() {
        return columns.toString();
    }
}
