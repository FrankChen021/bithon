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

package org.bithon.server.web.service.datasource.api;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 4/4/25 6:23 pm
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnarResponse {
    private String[] keys;
    private String[] values;
    private int rows;
    private Map<String, List<Object>> columns;

    public static ColumnarResponseBuilder builder() {
        return new ColumnarResponseBuilder();
    }

    public static class ColumnarResponseBuilder {
        private String[] keys;
        private String[] values;
        private Map<String, List<Object>> columns;
        private int rows;

        public ColumnarResponseBuilder keys(String... keys) {
            this.keys = keys;
            return this;
        }

        public ColumnarResponseBuilder keys(List<String> keys) {
            this.keys = keys.toArray(new String[0]);
            return this;
        }

        public ColumnarResponseBuilder values(List<String> values) {
            this.values = values.toArray(new String[0]);
            return this;
        }

        public ColumnarResponseBuilder values(String... values) {
            this.values = values;
            return this;
        }

        public ColumnarResponseBuilder columns(Map<String, List<Object>> columns) {
            this.columns = columns;
            if (!columns.isEmpty()) {
                List<Object> values = columns.entrySet().iterator().next().getValue();
                this.rows = values.size();
            }
            return this;
        }

        public ColumnarResponseBuilder rows(int rows) {
            this.rows = rows;
            return this;
        }

        public ColumnarResponse build() {
            return new ColumnarResponse(keys, values, rows, columns);
        }
    }
}
