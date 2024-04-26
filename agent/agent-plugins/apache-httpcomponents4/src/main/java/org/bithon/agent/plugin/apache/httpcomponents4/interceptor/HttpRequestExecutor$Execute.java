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

package org.bithon.agent.plugin.apache.httpcomponents4.interceptor;


import org.apache.http.Header;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.protocol.HttpContext;
import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.config.TraceConfig;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;

import java.util.Locale;

/**
 * {@link org.apache.http.protocol.HttpRequestExecutor#execute(HttpRequest, HttpClientConnection, HttpContext)}
 *
 * @author frankchen
 */
public class HttpRequestExecutor$Execute extends AroundInterceptor {

    private final TraceConfig traceConfig = ConfigurationManager.getInstance().getConfig(TraceConfig.class);

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        HttpRequest httpRequest = (HttpRequest) aopContext.getArgs()[0];

        //
        // Trace
        //
        ITraceSpan span = TraceContextFactory.newSpan("http-client");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        String uri;
        if (httpRequest instanceof HttpRequestWrapper) {
            // this getUri returns the full URI with host
            uri = ((HttpRequestWrapper) httpRequest).getOriginal().getRequestLine().getUri();
        } else {
            // this getUri returns the path and parameters, no host is included
            uri = httpRequest.getRequestLine().getUri();
        }

        // create a span and save it in user-context
        aopContext.setSpan(span.method(aopContext.getTargetClass(), aopContext.getMethod())
                               .kind(SpanKind.CLIENT)
                               .tag(Tags.Http.CLIENT, "apache-httpcomponents-4")
                               .tag(Tags.Http.URL, uri)
                               .tag(Tags.Http.METHOD, httpRequest.getRequestLine().getMethod())
                               .propagate(httpRequest, HttpMessage::setHeader)
                               .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext context) {
        ITraceSpan thisSpan = context.getSpan();
        if (thisSpan == null) {
            // in case of exception in the above
            return;
        }

        HttpResponse response = context.getReturningAs();
        String status = response == null ? "-1" : (response.getStatusLine() == null ? "-1" : Integer.toString(response.getStatusLine().getStatusCode()));
        thisSpan.tag(Tags.Http.STATUS, status)
                .tag(context.getException())
                .configIfTrue(response != null && !traceConfig.getHeaders().getResponse().isEmpty(),
                              (s) -> {
                                  for (String name : traceConfig.getHeaders().getResponse()) {
                                      Header header = response.getFirstHeader(name);
                                      if (header != null) {
                                          s.tag(Tags.Http.RESPONSE_HEADER_PREFIX + name.toLowerCase(Locale.ENGLISH), header.getValue());
                                      }
                                  }
                              })
                .finish();
    }
}
