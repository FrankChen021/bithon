/*
 *    Copyright 2020 bithon.cn
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

package org.bithon.server.metric.filter;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.bithon.server.metric.input.InputRow;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/19
 */
public class OrFilter implements IFilter {

    @NotNull
    private final List<IFilter> filters;

    public OrFilter(@JsonProperty("filters") List<IFilter> filters) {
        this.filters = filters;
    }

    @Override
    public boolean shouldInclude(InputRow inputRow) {
        for (IFilter filter : this.filters) {
            if (filter.shouldInclude(inputRow)) {
                return true;
            }
        }
        return false;
    }
}
