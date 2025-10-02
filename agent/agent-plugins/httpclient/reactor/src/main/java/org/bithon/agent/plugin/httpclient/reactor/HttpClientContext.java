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

package org.bithon.agent.plugin.httpclient.reactor;

import org.bithon.agent.observability.tracing.context.ITraceSpan;

/**
 * @author frank.chen021@outlook.com
 * @date 27/11/21 3:21 pm
 */
public class HttpClientContext {

    /**
     * Set the default value to the current time.
     * It can be overridden by the actual start time of the request.
     * However, when an exception is raised, this default value will be used to calculate the duration.
     */
    private long startTimeNs = System.nanoTime();

    /**
     * available when tracing is enabled on this request
     */
    private ITraceSpan span;

    private String uri;
    private String method;

    public HttpClientContext() {
    }

    public HttpClientContext(String uri, String method) {
        this.uri = uri;
        this.method = method;
    }

    public long getStartTimeNs() {
        return startTimeNs;
    }

    public void setStartTimeNs(long startTimeNs) {
        this.startTimeNs = startTimeNs;
    }

    public ITraceSpan getSpan() {
        return span;
    }

    public void setSpan(ITraceSpan span) {
        this.span = span;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
    public void setMethod(String method) {
        this.method = method;
    }

    public HttpClientContext withUri(String uri) {
        return new HttpClientContext(uri, this.method);
    }

    public HttpClientContext withMethod(String method) {
        return new HttpClientContext(this.uri, method);
    }

    public String getMethod() {
        return method;
    }

    public String getUri() {
        return uri;
    }
}
