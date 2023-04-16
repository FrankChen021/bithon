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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.bithon.server.storage.datasource.spec.IMetricSpec;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/16 14:42
 */
@Data
public class TimeSeriesMetric {
    private final List<String> tags;

    /**
     * Actual type of values is either double or long.
     * {@link Number} is not used because if an element is not set, the serialized value is null.
     * Since we want to keep the serialized value to be 0, the raw number type is the best
     */
    private final Object values;

    @JsonIgnore
    private BiConsumer<Integer, Object> valueSetter;

    @JsonIgnore
    private Function<Integer, Number> valueGetter;

    public TimeSeriesMetric(List<String> tags, int size, IMetricSpec metricSpec) {
        this.tags = tags;

        // by using double[] or long[], the empty slots are default to zero
//        if (metricSpec == null || metricSpec.getValueType() instanceof DoubleValueType) {
            this.values = new double[size + 1];
            this.valueSetter = (index, number) -> ((double[]) values)[index] = number == null ? 0 : ((Number) number).doubleValue();
            this.valueGetter = (index) -> ((double[]) values)[index];
//        } else {
//            this.values = new long[size + 1];
//            this.valueSetter = (index, number) -> ((long[]) values)[index] = number == null ? 0 : ((Number) number).longValue();
//            this.valueGetter = (index) -> ((long[]) values)[index];
//        }

    }

    public void set(int index, Object value) {
        this.valueSetter.accept(index, value);
    }

    public Number get(int index) {
        return this.valueGetter.apply(index);
    }
}
