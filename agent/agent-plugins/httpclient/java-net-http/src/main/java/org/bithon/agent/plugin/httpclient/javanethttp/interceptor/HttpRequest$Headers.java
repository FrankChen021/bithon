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

package org.bithon.agent.plugin.httpclient.javanethttp.interceptor;


import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.context.TraceMode;

import java.net.http.HttpHeaders;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link jdk.internal.net.http.HttpRequestImpl#headers()}
 * {@link jdk.internal.net.http.ImmutableHttpRequest#headers()}
 *
 * @author frank.chen021@outlook.com
 * @date 11/9/25 6:22 pm
 */
public class HttpRequest$Headers extends AfterInterceptor {
    @Override
    public void after(AopContext aopContext) {
        if (aopContext.hasException()) {
            return;
        }
        ITraceContext ctx = TraceContextHolder.current();
        if (ctx == null || ctx.traceMode() == TraceMode.LOGGING) {
            return;
        }
        HttpHeaders headers = aopContext.getReturningAs();

        final Map<String, List<String>> headerMap = new HashMap<>(headers.map());
        ctx.propagate(headerMap, (carrier, key, value) -> carrier.put(key, List.of(value)));

        aopContext.setReturning(HttpHeaders.of(headerMap, (a, b) -> true));
    }
}
