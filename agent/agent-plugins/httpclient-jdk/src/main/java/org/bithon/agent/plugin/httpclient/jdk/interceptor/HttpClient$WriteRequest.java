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

package org.bithon.agent.plugin.httpclient.jdk.interceptor;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.tracing.context.ITraceSpan;
import org.bithon.agent.core.tracing.context.SpanKind;
import org.bithon.agent.core.tracing.context.Tags;
import org.bithon.agent.core.tracing.context.TraceSpanFactory;
import sun.net.www.MessageHeader;

import java.net.HttpURLConnection;

/**
 * @author frankchen
 */
public class HttpClient$WriteRequest extends AbstractInterceptor {

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        MessageHeader headers = (MessageHeader) aopContext.getArgs()[0];

        ITraceSpan span = TraceSpanFactory.newSpan("httpClient-jdk");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        IBithonObject injectedObject = aopContext.castTargetAs();
        HttpURLConnection connection = (HttpURLConnection) injectedObject.getInjectedObject();

        /*
         * starts a span which will be finished after HttpClient.parseHttp
         */
        aopContext.setUserContext(span.method(aopContext.getMethod())
                                      .kind(SpanKind.CLIENT)
                                      .tag(Tags.URI, connection.getURL().toString())
                                      .tag(Tags.HTTP_METHOD, connection.getRequestMethod())
                                      .propagate(headers, (headersArgs, key, value) -> headersArgs.set(key, value))
                                      .start());

        return InterceptionDecision.CONTINUE;
    }
}
