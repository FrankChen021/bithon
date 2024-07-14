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

package org.bithon.server.pipeline.metrics.input;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.pipeline.common.transform.TransformSpec;
import org.bithon.server.storage.datasource.ISchema;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 12/4/22 11:20 AM
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface IMetricInputSource {

    TransformSpec getTransformSpec();

    /**
     * Start ingestion
     */
    void start(ISchema schema);

    /**
     * Stop ingestion
     */
    void stop();

    /**
     * Sample ingestion
     * @return the sample data
     */
    List<Map<String, Object>> sample(ISchema schema, Duration timeout);
}
