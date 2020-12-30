package com.sbss.bithon.agent.plugin.jdk.httpclient;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.IBithonObject;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;
import com.sbss.bithon.agent.core.tracing.context.SpanKind;
import com.sbss.bithon.agent.core.tracing.context.TraceContext;
import com.sbss.bithon.agent.core.tracing.context.TraceContextHolder;
import com.sbss.bithon.agent.core.tracing.context.TraceSpan;
import sun.net.www.MessageHeader;

import java.net.HttpURLConnection;

/**
 * @author frankchen
 */
public class HttpClientWriteRequestInterceptor extends AbstractInterceptor {


    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        TraceContext traceContext = TraceContextHolder.get();
        if (traceContext == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }
        TraceSpan span = traceContext.currentSpan();
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        IBithonObject injectedObject = aopContext.castTargetAs();
        HttpURLConnection connection = (HttpURLConnection) injectedObject.getInjectedObject();

        /*
         * starts a span which will be finished after HttpClient.parseHttp
         */
        aopContext.setUserContext(span.newChildSpan("httpClient")
                                      .clazz(aopContext.getTargetClass())
                                      .method(aopContext.getMethod())
                                      .kind(SpanKind.CLIENT)
                                      .tag("uri", connection.getURL().toString())
                                      .start());

        //
        // propagate tracing after span creation
        //
        MessageHeader headers = (MessageHeader) aopContext.getArgs()[0];
        traceContext.propagate(headers, (headersArgs, key, value) -> {
            headersArgs.add(key, value);
        });

        return InterceptionDecision.CONTINUE;
    }
}
