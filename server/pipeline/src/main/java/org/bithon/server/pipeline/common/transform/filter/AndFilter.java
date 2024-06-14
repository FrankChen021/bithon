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

package org.bithon.server.pipeline.common.transform.filter;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.bithon.server.storage.datasource.input.IInputRow;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 4/8/22 3:31 PM
 */
public class AndFilter implements IInputRowFilter {

    @Getter
    private final List<IInputRowFilter> filters;

    public AndFilter(@JsonProperty("filters") List<IInputRowFilter> filters) {
        this.filters = filters;
    }

    @Override
    public boolean shouldInclude(IInputRow inputRow) {
        for (IInputRowFilter filter : this.filters) {
            if (!filter.shouldInclude(inputRow)) {
                return false;
            }
        }
        return true;
    }
}
