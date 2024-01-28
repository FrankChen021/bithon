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

package org.bithon.server.pipeline.tracing.exporter;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.pipeline.common.pipeline.IExporter;
import org.bithon.server.pipeline.tracing.ITraceProcessor;

/**
 * @author frank.chen021@outlook.com
 * @date 9/12/21 2:22 PM
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = "store", value = ToTraceStorageExporter.class),
    @JsonSubTypes.Type(name = "kafka", value = ToKafkaExporter.class),
})
public interface ITraceExporter extends ITraceProcessor, IExporter {

    default void start() {

    }

    default void stop() {
        try {
            close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
