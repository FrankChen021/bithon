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

import org.bithon.agent.observability.metric.model.Sum;
import org.bithon.agent.observability.tracing.context.ITraceSpan;

/**
 * Context object to track HTTP client metrics and tracing information
 * 
 * @author frank.chen021@outlook.com
 * @date 2024/12/19
 */
public class HttpClientContext {
    private String url;
    private String method;
    private final Sum sentBytes;
    private final Sum receiveBytes;
    private long requestStartTime;
    private int responseCode;
    private ITraceSpan traceSpan;

    public HttpClientContext() {
        this.sentBytes = new Sum();
        this.receiveBytes = new Sum();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Sum getSentBytes() {
        return sentBytes;
    }

    public Sum getReceiveBytes() {
        return receiveBytes;
    }

    public long getRequestStartTime() {
        return requestStartTime;
    }

    public void setRequestStartTime(long requestStartTime) {
        this.requestStartTime = requestStartTime;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public ITraceSpan getTraceSpan() {
        return traceSpan;
    }

    public void setTraceSpan(ITraceSpan traceSpan) {
        this.traceSpan = traceSpan;
    }
}
