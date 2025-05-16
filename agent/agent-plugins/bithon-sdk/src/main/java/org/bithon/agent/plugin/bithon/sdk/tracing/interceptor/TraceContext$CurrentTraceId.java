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

package org.bithon.agent.plugin.bithon.sdk.tracing.interceptor;


import org.bithon.agent.instrumentation.aop.interceptor.declaration.ReplaceInterceptor;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;

/**
 * @author frank.chen021@outlook.com
 * @date 14/5/25 8:54 pm
 */
public class TraceContext$CurrentTraceId extends ReplaceInterceptor {
    @Override
    public Object execute(Object[] args, Object returning) {
        return TraceContextHolder.currentTraceId();
    }
}
