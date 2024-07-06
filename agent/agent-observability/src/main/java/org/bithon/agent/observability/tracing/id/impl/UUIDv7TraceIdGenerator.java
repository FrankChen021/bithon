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
import org.bithon.component.commons.uuid.UUIDv7Generator;

/**
 * @author frank.chen021@outlook.com
 * @date 14/5/24 10:52 pm
 */
public class UUIDv7TraceIdGenerator implements ITraceIdGenerator {
    // Use plus_1 version to get better monotonicity
    private final UUIDv7Generator generator;

    public UUIDv7TraceIdGenerator(int type) {
        this.generator = UUIDv7Generator.create(UUIDv7Generator.INCREMENT_TYPE_DEFAULT);
    }

    @Override
    public String newId() {
        return generator.generate().toCompactFormat();
    }
}
