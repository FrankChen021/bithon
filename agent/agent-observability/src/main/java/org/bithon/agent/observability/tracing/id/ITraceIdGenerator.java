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

package org.bithon.agent.observability.tracing.id;

import org.bithon.agent.observability.tracing.id.impl.UUIDv4TraceIdGenerator;
import org.bithon.agent.observability.tracing.id.impl.UUIDv7Plus1TraceIdGenerator;
import org.bithon.agent.observability.tracing.id.impl.UUIDv7TraceIdGenerator;
import org.bithon.agent.observability.tracing.id.impl.UUIDv7TracePlusNTraceIdGenerator;
import org.bithon.shaded.com.fasterxml.jackson.annotation.JsonSubTypes;
import org.bithon.shaded.com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 12:04 上午
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = UUIDv7TraceIdGenerator.class)
@JsonSubTypes(value = {
    @JsonSubTypes.Type(name = "uuidv4", value = UUIDv4TraceIdGenerator.class),
    @JsonSubTypes.Type(name = "uuidv7", value = UUIDv7TraceIdGenerator.class),
    @JsonSubTypes.Type(name = "uuidv7-1", value = UUIDv7Plus1TraceIdGenerator.class),
    @JsonSubTypes.Type(name = "uuidv7-n", value = UUIDv7TracePlusNTraceIdGenerator.class),
})
public interface ITraceIdGenerator {
    String newId();
}
