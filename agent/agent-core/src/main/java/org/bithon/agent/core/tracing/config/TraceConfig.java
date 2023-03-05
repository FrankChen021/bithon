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

package org.bithon.agent.core.tracing.config;

import org.bithon.agent.core.config.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/5 21:33
 */
@ConfigurationProperties(prefix = "tracing")
public class TraceConfig {

    public static class HeaderConfig {
        private List<String> request = Collections.emptyList();
        private List<String> response = Collections.emptyList();

        public List<String> getRequest() {
            return request;
        }

        public void setRequest(List<String> request) {
            this.request = request;
        }

        public List<String> getResponse() {
            return response;
        }

        public void setResponse(List<String> response) {
            this.response = response;
        }
    }

    /**
     * Use for debug.
     * Under debug mode, events of span creation and completion will be logged
     */
    private boolean debug = false;

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * If this field is set, the trace id(if current request has) will be added to the header of response.
     * The header name is the value of this field, header value is the trace id.
     */
    private String traceIdInResponse;

    public String getTraceIdInResponse() {
        return traceIdInResponse;
    }

    public void setTraceIdInResponse(String traceIdInResponse) {
        this.traceIdInResponse = traceIdInResponse;
    }

    /**
     * Assigne a value so that user code won't check if it's null
     */
    private HeaderConfig headers = new HeaderConfig();

    public HeaderConfig getHeaders() {
        return headers;
    }

    public void setHeaders(HeaderConfig headers) {
        this.headers = headers;
    }
}
