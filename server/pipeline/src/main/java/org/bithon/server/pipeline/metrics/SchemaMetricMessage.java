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

package org.bithon.server.pipeline.metrics;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.input.IInputRow;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 3/10/21 14:07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaMetricMessage {
    /**
     * It's nullable because not all messages(like some predefined metrics such as jvm-metrics) contains schema
     */
    @Nullable
    private DataSourceSchema schema;

    /**
     * Since we support extract metrics from TraceSpan, we here use {@link IInputRow} as type declaration instead of {@link MetricMessage}.
     * But for deserialization, we always deserialize the input as {@link MetricMessage}
     */
    @JsonDeserialize(using = MetricMessageListDeserializer.class)
    private List<IInputRow> metrics;
}
