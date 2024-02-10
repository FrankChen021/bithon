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

package org.bithon.agent.observability.tracing.config;

import org.bithon.agent.configuration.ConfigurationProperties;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.shaded.com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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
            // Headers in HTTP protocols are case-insensitive
            this.request = request.stream()
                                  .map((header) -> header.toLowerCase(Locale.ENGLISH))
                                  .collect(Collectors.toList());
        }

        public List<String> getResponse() {
            return response;
        }

        public void setResponse(List<String> response) {
            // Headers in HTTP protocols are case-insensitive
            this.response = response.stream()
                                    .map((header) -> header.toLowerCase(Locale.ENGLISH))
                                    .collect(Collectors.toList());
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

    private boolean disabled = false;

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * If this field is set, the trace id (if the current request has) will be added to the header of response.
     * The header name is the value of this field, header value is the trace id.
     * For example, X-Bithon-TraceId
     */
    private String traceResponseHeader = "X-Bithon-Trace";

    @JsonIgnore
    private String traceIdResponseHeader = "X-Bithon-Trace-Id";

    @JsonIgnore
    private String traceModeResponseHeader = "X-Bithon-Trace-Mode";

    public String getTraceIdResponseHeader() {
        return traceIdResponseHeader;
    }

    public String getTraceModeResponseHeader() {
        return traceModeResponseHeader;
    }

    public String getTraceResponseHeader() {
        return traceResponseHeader;
    }

    public void setTraceResponseHeader(String traceResponseHeader) {
        this.traceResponseHeader = traceResponseHeader;
        this.traceIdResponseHeader = StringUtils.hasText(traceResponseHeader) ? traceResponseHeader + "-Id" : null;
        this.traceModeResponseHeader = StringUtils.hasText(traceResponseHeader) ? traceResponseHeader + "-Mode" : null;
    }

    /**
     * Assign a value so that user code won't check if it's null
     */
    private HeaderConfig headers = new HeaderConfig();

    public HeaderConfig getHeaders() {
        return headers;
    }

    public void setHeaders(HeaderConfig headers) {
        this.headers = headers;
    }
}
