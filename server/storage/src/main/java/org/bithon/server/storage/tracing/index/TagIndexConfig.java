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

package org.bithon.server.storage.tracing.index;

import lombok.Data;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 3/3/22 11:54 AM
 */
@Data
public class TagIndexConfig {
    /**
     * For which tags we're building indexes.
     * <p>
     * Currently, we define a list of tag names to make this index module work in minimal work.
     * <p>
     * Since the names may be the same in different span logs, if we only want to build indexes for some specific span logs,
     * a filter for span is needed. That might be the future work if necessary.
     * <p>
     * key: tag name
     * val: column name in bithon_trace_span_tag_index table ranging from 1 to 16. Different tag names SHOULD NOT share a same column name.
     * <p>
     * LinkedHashMap is used to keep the order of the configuration
     */
    private Map<String, Integer> map;

    /**
     * Get the column position for given tag name.
     * The position should start from 1, not 0.
     * If it's zero, it means there's no index configured for this tag
     */
    public int getColumnPos(String tagName) {
        return map.getOrDefault(tagName, 0);
    }
}
