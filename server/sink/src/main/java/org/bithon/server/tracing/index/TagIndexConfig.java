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

package org.bithon.server.tracing.index;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * @author Frank Chen
 * @date 3/3/22 11:54 AM
 */
@Data
public class TagIndexConfig {
    /**
     * for which tags we're building indexes.
     *
     * Currently, we define a list of tag names to make this index module work in minimal work.
     *
     * Since the names may be the same in different span logs, if we only want to build indexes for some specific span logs,
     * a filter for span is needed. That might be the future work if necessary.
     */
    private List<String> names = Collections.emptyList();
}
