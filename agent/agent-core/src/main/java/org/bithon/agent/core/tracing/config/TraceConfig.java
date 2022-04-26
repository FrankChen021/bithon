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

/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/5 21:33
 */
@ConfigurationProperties(prefix = "tracing")
public class TraceConfig {

    /**
     * in range of [0, 100]
     */
    @Range(min = 0, max = 100)
    private int samplingRate = 0;

    /**
     * Use for debug. Log events of span creation and finish
     */
    private boolean logSpans = false;

    public boolean isLogSpans() {
        return logSpans;
    }

    public void setLogSpans(boolean logSpans) {
        this.logSpans = logSpans;
    }

    public List<String> getHeaders() {
        return headers;
    }

    /**
     * headers that should be recorded in trace span
     */
    private final List<String> headers = Collections.emptyList();

    // TODO: separated disable from samplingRate value
    // because even when samplingRate is zero, it can be traced if upstream application passes related headers
    public boolean isDisabled() {
        return samplingRate == 0;
    }

    public int getSamplingRate() {
        return samplingRate;
    }

    public void setSamplingRate(int samplingRate) {
        this.samplingRate = samplingRate;
    }


}
