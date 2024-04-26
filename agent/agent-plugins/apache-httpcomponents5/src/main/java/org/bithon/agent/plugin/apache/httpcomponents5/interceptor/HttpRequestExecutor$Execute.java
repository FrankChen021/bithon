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

package org.bithon.agent.plugin.apache.httpcomponents5.interceptor;


import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpMessage;
import org.apache.hc.core5.http.io.HttpClientConnection;
import org.apache.hc.core5.http.io.HttpResponseInformationCallback;
import org.apache.hc.core5.http.protocol.HttpContext;
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
 * {@link org.apache.hc.core5.http.impl.io.HttpRequestExecutor#execute(ClassicHttpRequest, HttpClientConnection, HttpResponseInformationCallback, HttpContext)}
 *
 * @author frankchen
 */
public class HttpRequestExecutor$Execute extends AroundInterceptor {

    private final TraceConfig traceConfig = ConfigurationManager.getInstance().getConfig(TraceConfig.class);

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ITraceSpan span = TraceContextFactory.newSpan("http-client");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        ClassicHttpRequest httpRequest = (ClassicHttpRequest) aopContext.getArgs()[0];
        aopContext.setSpan(span.method(aopContext.getTargetClass(), aopContext.getMethod())
                               .kind(SpanKind.CLIENT)
                               .tag(Tags.Http.CLIENT, "apache-httpcomponents-5")
                               .tag(Tags.Http.URL, httpRequest.getRequestUri())
                               .tag(Tags.Http.METHOD, httpRequest.getMethod())
                               .propagate(httpRequest, HttpMessage::addHeader)
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

        thisSpan.tag(context.getException());
        ClassicHttpResponse response = context.getReturningAs();
        if (response != null) {
            thisSpan.tag(Tags.Http.STATUS, String.valueOf(response.getCode()))
                    .configIfTrue(!traceConfig.getHeaders().getResponse().isEmpty(),
                                  (s) -> {
                                      for (String name : traceConfig.getHeaders().getResponse()) {
                                          Header header = response.getFirstHeader(name);
                                          if (header != null) {
                                              s.tag(Tags.Http.RESPONSE_HEADER_PREFIX + name.toLowerCase(Locale.ENGLISH), header.getValue());
                                          }
                                      }
                                  });
        }
        thisSpan.finish();
    }
}
