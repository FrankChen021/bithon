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

package org.bithon.agent.core.tracing.context;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 8:51 下午
 */
public enum SpanKind {
    /**
     * a client is a termination of trace in current context.
     * It spreads the trace context to next hop.
     *
     * For such type, 'targetType' and 'uri' must be filled in `tags`
     */
    CLIENT,
    SERVER,
    PRODUCER,
    CONSUMER;

    SpanKind() {
    }
}
