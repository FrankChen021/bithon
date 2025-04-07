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

package org.bithon.server.metric.expression.evaluation;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/// |appName | value |
/// |-----------------------|-----------------|
/// | {k1} | value1 |
/// | {k2} | value2 |
///
/// List | Map<String, List>
///
/// |appName, instanceName | metric1 | metric2 |
/// |-----------------------|--------|---------|
/// | {key1, key2} | value1 | value2 |
/// | {key3, key4} | value3 | value4 |
///
///
/// @author frank.chen021@outlook.com
/// @date 4/4/25 6:23 pm
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {
    private int rows;

    private String[] keyNames;
    private List<List<Object>> keys;

    private String[] valueNames;
    private Map<String, List<Object>> values;

    private long startTimestamp;
    private long endTimestamp;
    private long interval;

    public static ColumnarResponseBuilder builder() {
        return new ColumnarResponseBuilder();
    }

    public static class ColumnarResponseBuilder {
        private String[] keyNames;
        private String[] valueNames;
        private List<List<Object>> keys;
        private Map<String, List<Object>> values;
        private int rows;
        private long startTimestamp;
        private long endTimestamp;
        private long interval;

        public ColumnarResponseBuilder keyNames(String... keyNames) {
            this.keyNames = keyNames;
            return this;
        }

        public ColumnarResponseBuilder keyNames(List<String> keyNames) {
            this.keyNames = keyNames.toArray(new String[0]);
            return this;
        }

        public ColumnarResponseBuilder valueNames(List<String> valueNames) {
            this.valueNames = valueNames.toArray(new String[0]);
            return this;
        }

        public ColumnarResponseBuilder valueNames(String... values) {
            this.valueNames = values;
            return this;
        }

        public ColumnarResponseBuilder keys(List<List<Object>> keys) {
            this.keys = keys;
            if (!keys.isEmpty()) {
                this.rows = keys.size();
            }
            return this;
        }

        public ColumnarResponseBuilder values(Map<String, List<Object>> values) {
            this.values = values;
            if (!values.isEmpty()) {
                List<Object> col = values.entrySet().iterator().next().getValue();
                this.rows = col.size();
            }
            return this;
        }

        public ColumnarResponseBuilder rows(int rows) {
            this.rows = rows;
            return this;
        }

        public ColumnarResponseBuilder startTimestamp(long startTimestamp) {
            this.startTimestamp = startTimestamp;
            return this;
        }

        public ColumnarResponseBuilder endTimestamp(long endTimestamp) {
            this.endTimestamp = endTimestamp;
            return this;
        }

        public ColumnarResponseBuilder interval(long interval) {
            this.interval = interval;
            return this;
        }

        public EvaluationResult build() {
            return new EvaluationResult(rows, keyNames, keys, valueNames, values, startTimestamp, endTimestamp, interval);
        }
    }
}
