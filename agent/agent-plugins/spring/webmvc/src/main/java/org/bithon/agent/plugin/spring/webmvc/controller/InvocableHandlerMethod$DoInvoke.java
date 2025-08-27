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

package org.bithon.agent.plugin.spring.webmvc.controller;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptionDecision;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link org.springframework.web.method.support.InvocableHandlerMethod#doInvoke(Object...)}
 *
 * @author frank.chen021@outlook.com
 * @date 2024/5/8 16:16
 */
public class InvocableHandlerMethod$DoInvoke extends AroundInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        ITraceSpan span = TraceContextFactory.newSpan("spring-controller");
        if (span == null) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        InvocableHandlerMethod handler = aopContext.getTargetAs();
        aopContext.setSpan(span.method(handler.getMethod())
                               .start());

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        ITraceSpan span = aopContext.getSpan();
        span.tag(aopContext.getException()).finish();
        
        // Handle StreamingResponseBody wrapping
        wrapStreamingResponseBodyWithTracing(aopContext, span);
    }
    
    /**
     * Wrap StreamingResponseBody return values with tracing context
     */
    private void wrapStreamingResponseBodyWithTracing(AopContext aopContext, ITraceSpan parentSpan) {
        Object returnValue = aopContext.getReturning();

        if (returnValue instanceof StreamingResponseBody) {
            StreamingResponseBody delegate = (StreamingResponseBody) returnValue;
            TracedStreamingResponseBody wrapper = new TracedStreamingResponseBody(delegate, parentSpan.context()
                                                                                                      .copy()
                                                                                                      .newSpan(parentSpan.spanId()));
            aopContext.setReturning(wrapper);
        } else if (returnValue instanceof ResponseEntity) {
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) returnValue;
            Object body = responseEntity.getBody();
            
            if (body instanceof StreamingResponseBody) {
                StreamingResponseBody delegate = (StreamingResponseBody) body;
                TracedStreamingResponseBody wrapper = new TracedStreamingResponseBody(delegate, parentSpan.context()
                                                                                                          .copy()
                                                                                                          .newSpan(parentSpan.spanId()));
                
                // Create new ResponseEntity with wrapped body
                ResponseEntity<StreamingResponseBody> newResponseEntity = ResponseEntity
                    // Can't use getStatus since it does not exist in higher versions of Spring
                    .status(responseEntity.getStatusCodeValue())
                    .headers(responseEntity.getHeaders())
                    .contentType(responseEntity.getHeaders().getContentType())
                    .body(wrapper);
                
                aopContext.setReturning(newResponseEntity);
            }
        }
    }
}

/**
 * Wrapper for StreamingResponseBody that preserves tracing context
 * across thread boundaries during streaming operations.
 */
class TracedStreamingResponseBody implements StreamingResponseBody {
    
    private final StreamingResponseBody delegate;
    private final ITraceSpan span;
    
    public TracedStreamingResponseBody(StreamingResponseBody delegate, ITraceSpan span) {
        this.delegate = delegate;
        this.span = span;
    }
    
    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        span.name("StreamingResponseBody")
            .method(this.delegate.getClass(), "writeTo")
            .start();

        TraceContextHolder.attach(span.context());
        try {
            delegate.writeTo(outputStream);
        } catch (Throwable e) {
            span.tag(e);
            throw e;
        } finally {
            TraceContextHolder.detach();

            span.finish();
            span.context().finish();
        }
    }
}
