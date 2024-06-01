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
import org.bithon.agent.observability.tracing.id.impl.UUIDv7TraceIdGenerator;
import org.bithon.component.commons.uuid.UUIDv7Generator;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/6/1 22:25
 */
public class TraceIdGeneratorFactory {
    public static ITraceIdGenerator create(String type) {
        switch (type) {
            case "uuidv4":
                return new UUIDv4TraceIdGenerator();

            case "uuidv7":
                return new UUIDv7TraceIdGenerator(UUIDv7Generator.INCREMENT_TYPE_DEFAULT);

            case "uuidv7-1":
                return new UUIDv7TraceIdGenerator(UUIDv7Generator.INCREMENT_TYPE_PLUS_1);

            case "uuidv7-n":
                return new UUIDv7TraceIdGenerator(UUIDv7Generator.INCREMENT_TYPE_PLUS_N);

            default:
                throw new IllegalArgumentException("Unsupported trace id generator type: " + type);
        }
    }
}
