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

package org.bithon.server.web.service.tracing.api;

import lombok.Data;
import org.bithon.server.storage.tracing.TraceSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 3:31 下午
 */
@Data
public class TraceSpanBo extends TraceSpan {

    public List<TraceSpanBo> children = new ArrayList<>();

    /**
     * if children is not set, then this field is used to store the child indexes
     */
    public List<Integer> childRefs = new ArrayList<>();

    /**
     * A property that will be sent to clients
     */
    public int getChildCount() {
        // Since only children or childRefs is set, it's safe to add them together
        return children.size() + childRefs.size();
    }

    public int depth = 0;

    /**
     * row index in the array
     */
    public int index = 0;

    /**
     * The gap between the start time of this span and the end time of the previous span in microseconds
     */
    public long gap = 0;

    public String unQualifiedClassName;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        TraceSpanBo that = (TraceSpanBo) o;
        return Objects.equals(children, that.children);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), children);
    }
}
