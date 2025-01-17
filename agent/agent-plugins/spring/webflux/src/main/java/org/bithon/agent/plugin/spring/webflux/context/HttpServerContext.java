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

import org.bithon.agent.observability.metric.domain.httpclient.HttpIOMetrics;
import org.bithon.agent.observability.tracing.context.ITraceContext;

/**
 * @author frank.chen021@outlook.com
 * @date 27/11/21 2:41 pm
 */
public class HttpServerContext {
    private final HttpIOMetrics metrics = new HttpIOMetrics();
    private ITraceContext traceContext;

    public HttpIOMetrics getMetrics() {
        return metrics;
    }

    public ITraceContext getTraceContext() {
        return traceContext;
    }

    public void setTraceContext(ITraceContext traceContext) {
        this.traceContext = traceContext;
    }
}
