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

package org.bithon.server.sink.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bithon.component.commons.collection.IteratorableCollection;
import org.bithon.server.storage.datasource.DataSourceSchema;

/**
 * @author Frank Chen
 * @date 3/10/21 14:07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchemaMetricMessage {
    private DataSourceSchema schema;
    private IteratorableCollection<MetricMessage> metrics;
}
