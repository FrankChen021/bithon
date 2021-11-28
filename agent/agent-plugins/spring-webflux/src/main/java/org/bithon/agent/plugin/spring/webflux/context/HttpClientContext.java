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

package org.bithon.agent.plugin.spring.webflux.context;

import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.ITraceSpan;

/**
 * @author Frank Chen
 * @date 27/11/21 3:21 pm
 */
public class HttpClientContext {

    private final long startTimeNs;

    /**
     * URI including host,port,path
     */
    private final String uri;
    private String method;

    /**
     * available when tracing is enabled on this request
     */
    private ITraceContext traceContext;
    private ITraceSpan span;

    public HttpClientContext(String uri) {
        this.startTimeNs = System.nanoTime();
        this.uri = uri;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public long getStartTimeNs() {
        return startTimeNs;
    }

    public String getUri() {
        return uri;
    }

    public ITraceContext getTraceContext() {
        return traceContext;
    }

    public void setTraceContext(ITraceContext traceContext) {
        this.traceContext = traceContext;
    }

    public ITraceSpan getSpan() {
        return span;
    }

    public void setSpan(ITraceSpan span) {
        this.span = span;
    }
}
