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

package org.bithon.agent.plugin.httpserver.jetty12.context;

import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.eclipse.jetty.server.internal.HttpChannelState;

/**
 * @author frank.chen021@outlook.com
 * @date 7/12/21 9:21 PM
 */
public class RequestContext {
    private final HttpChannelState.ChannelRequest channelRequest;
    private final HttpChannelState.ChannelResponse channelResponse;
    private final ITraceContext traceContext;

    public RequestContext(HttpChannelState.ChannelRequest request,
                          HttpChannelState.ChannelResponse response,
                          ITraceContext traceContext) {
        this.channelRequest = request;
        this.channelResponse = response;
        this.traceContext = traceContext;
    }

    public HttpChannelState.ChannelRequest getChannelRequest() {
        return channelRequest;
    }

    public HttpChannelState.ChannelResponse getChannelResponse() {
        return channelResponse;
    }

    /**
     * @return the tracing context that attached to one HTTP request. Can be NULL
     */
    public ITraceContext getTraceContext() {
        return traceContext;
    }
}
