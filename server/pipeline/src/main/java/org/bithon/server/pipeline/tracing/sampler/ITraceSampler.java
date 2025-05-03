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

package org.bithon.server.pipeline.tracing.sampler;

import org.bithon.server.datasource.ISchema;
import org.bithon.server.discovery.declaration.DiscoverableService;
import org.bithon.server.pipeline.metrics.input.IMetricInputSource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Internal API for the web service module to interact with.
 * <p>
 * In distribution deployment, the pipeline module and the web-service might be deployed in different JVMs,
 * to allow the web-service module to call the pipeline module,
 * we need to leverage the service-registration mechanism provided by the discovery module
 * so that the web-service module can call the API to sample data.
 *
 * @author frank.chen021@outlook.com
 * @date 2024/7/15 20:58
 */
@DiscoverableService(name = "pipeline-tracing-api")
public interface ITraceSampler {

    @PostMapping("/api/pipeline/tracing/sample")
    IMetricInputSource.SamplingResult sample(@RequestBody ISchema schema);
}
