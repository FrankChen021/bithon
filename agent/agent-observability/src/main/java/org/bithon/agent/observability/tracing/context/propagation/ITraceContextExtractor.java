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

package org.bithon.agent.observability.tracing.context.propagation;

import org.bithon.agent.observability.tracing.context.ITraceContext;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 9:39 下午
 */
public interface ITraceContextExtractor {
    /**
     * Extract tracing context from given request object
     * @param request the object that might contain incoming tracing context
     * @param getter the functional object that extract value from the given request object
     * @return tracing context. can be null
     */
    <R> ITraceContext extract(R request, PropagationGetter<R> getter);
}
