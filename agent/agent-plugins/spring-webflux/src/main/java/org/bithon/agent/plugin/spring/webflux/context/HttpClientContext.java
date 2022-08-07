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

import org.bithon.agent.core.tracing.context.ITraceSpan;

/**
 * @author frank.chen021@outlook.com
 * @date 27/11/21 3:21 pm
 */
public class HttpClientContext {

    private final long startTimeNs;

    /**
     * available when tracing is enabled on this request
     */
    private ITraceSpan span;

    public HttpClientContext() {
        this.startTimeNs = System.nanoTime();
    }

    public long getStartTimeNs() {
        return startTimeNs;
    }

    public ITraceSpan getSpan() {
        return span;
    }

    public void setSpan(ITraceSpan span) {
        this.span = span;
    }
}
