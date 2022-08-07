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

package org.bithon.agent.plugin.jetty.context;

import org.bithon.agent.core.tracing.context.ITraceContext;

/**
 * @author frank.chen021@outlook.com
 * @date 7/12/21 9:21 PM
 */
public class RequestContext {
    private final long startNanoTime;
    private final ITraceContext traceContext;

    public RequestContext(long startNanoTime, ITraceContext traceContext) {
        this.startNanoTime = startNanoTime;
        this.traceContext = traceContext;
    }

    public long getStartNanoTime() {
        return startNanoTime;
    }

    public ITraceContext getTraceContext() {
        return traceContext;
    }
}
