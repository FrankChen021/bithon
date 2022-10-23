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
import org.bithon.agent.core.config.validation.Range;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/5 21:33
 */
@ConfigurationProperties(prefix = "tracing")
public class TraceConfig {

    public static class SamplingConfig {
        /**
         * in range of [0, 100]
         */
        @Range(min = 0, max = 100)
        private int samplingRate = 0;

        private boolean disabled = false;

        public int getSamplingRate() {
            return samplingRate;
        }

        public void setSamplingRate(int samplingRate) {
            this.samplingRate = samplingRate;
        }

        public boolean isDisabled() {
            return disabled;
        }

        public void setDisabled(boolean disabled) {
            this.disabled = disabled;
        }
    }

    /**
     * Sampling configuration for different entries
     * key: entry name. Such as web/quartz/spring-scheduler
     */
    private Map<String, SamplingConfig> samplingConfigs = Collections.emptyMap();

    public Map<String, SamplingConfig> getSamplingConfigs() {
        return samplingConfigs;
    }

    public void setSamplingConfigs(Map<String, SamplingConfig> samplingConfigs) {
        this.samplingConfigs = samplingConfigs;
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

    public List<String> getHeaders() {
        return headers;
    }

    /**
     * headers that should be recorded in trace span
     */
    private final List<String> headers = Collections.emptyList();

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
}
