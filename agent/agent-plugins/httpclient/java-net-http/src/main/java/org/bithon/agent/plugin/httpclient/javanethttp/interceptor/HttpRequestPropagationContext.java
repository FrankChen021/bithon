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

import org.bithon.agent.instrumentation.aop.IBithonObject;
import org.bithon.agent.observability.tracing.context.ITraceContext;

import java.net.http.HttpRequest;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

final class HttpRequestPropagationContext {
    private static final Map<HttpRequest, ITraceContext> CONTEXTS = Collections.synchronizedMap(new WeakHashMap<>());

    private HttpRequestPropagationContext() {
    }

    static void set(HttpRequest request, ITraceContext context) {
        if (request instanceof IBithonObject) {
            ((IBithonObject) request).setInjectedObject(context);
        }
        CONTEXTS.put(request, context);
    }

    static ITraceContext get(Object request) {
        if (request instanceof IBithonObject) {
            Object injected = ((IBithonObject) request).getInjectedObject();
            if (injected instanceof ITraceContext) {
                return (ITraceContext) injected;
            }
        }
        if (request instanceof HttpRequest) {
            return CONTEXTS.get(request);
        }
        return null;
    }

    static void remove(HttpRequest request) {
        if (request instanceof IBithonObject) {
            ((IBithonObject) request).setInjectedObject(null);
        }
        CONTEXTS.remove(request);
    }
}
