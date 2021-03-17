package com.sbss.bithon.agent.plugin.httpclient.jdk;

import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.context.InterceptorContext;
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

    private String srcApplication;

    @Override
    public boolean initialize() throws Exception {
        srcApplication = AgentContext.getInstance().getAppInstance().getAppName();
        return super.initialize();
    }

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        MessageHeader headers = (MessageHeader) aopContext.getArgs()[0];
        headers.set(InterceptorContext.HEADER_SRC_APPLICATION_NAME, srcApplication);

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
        traceContext.propagate(headers, (headersArgs, key, value) -> {
            headersArgs.set(key, value);
        });

        return InterceptionDecision.CONTINUE;
    }
}
