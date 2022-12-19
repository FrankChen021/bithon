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

package org.bithon.agent.plugin.apache.kafka.producer;

import org.bithon.agent.core.config.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

/**
 * @author Frank Chen
 * @date 22/11/22 10:31 pm
 */
@ConfigurationProperties(prefix = "agent.plugin.kafka.producer.tracing")
public class KafkaProducerTracingConfig {

    /**
     * headers that will be recorded into tracing logs
     */
    private List<String> headers = Collections.emptyList();

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }
}
