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

package org.bithon.agent.observability.tracing.id.impl;

import org.bithon.agent.observability.tracing.id.ITraceIdGenerator;

import java.util.UUID;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 12:20 上午
 */
public class UUIDv4TraceIdGenerator implements ITraceIdGenerator {
    /**
     * generates an opentelemetry specification standard trace id
     */
    @Override
    public String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
