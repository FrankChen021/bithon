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

package org.bithon.server.tracing.storage;

import lombok.Data;
import org.bithon.server.common.ttl.TTLConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author Frank Chen
 * @date 17/10/21 10:44 am
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "bithon.storage.tracing")
public class TraceStorageConfig {
    private String type;
    private TTLConfig ttl;
    private int batchSize = 2000;
}
