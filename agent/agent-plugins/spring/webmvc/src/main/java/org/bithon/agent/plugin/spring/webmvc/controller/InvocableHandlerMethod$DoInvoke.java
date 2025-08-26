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
import org.bithon.agent.observability.tracing.context.ITraceContext;
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
        wrapStreamingResponseBodyWithTracing(aopContext);
    }
    
    /**
     * Wrap StreamingResponseBody return values with tracing context
     */
    private void wrapStreamingResponseBodyWithTracing(AopContext aopContext) {
        Object returnValue = aopContext.getReturning();
        if (returnValue == null) {
            return;
        }

        // Get current trace context before wrapping
        ITraceContext currentContext = TraceContextHolder.current();
        if (currentContext == null) {
            return;
        }

        // Copy context to avoid thread interference
        ITraceContext contextCopy = currentContext.copy();

        if (returnValue instanceof StreamingResponseBody) {
            // Direct StreamingResponseBody return
            StreamingResponseBody original = (StreamingResponseBody) returnValue;
            TracingStreamingResponseBodyWrapper wrapper = new TracingStreamingResponseBodyWrapper(original, contextCopy);
            aopContext.setReturning(wrapper);
            
        } else if (returnValue instanceof ResponseEntity) {
            // ResponseEntity<StreamingResponseBody> return
            ResponseEntity<?> responseEntity = (ResponseEntity<?>) returnValue;
            Object body = responseEntity.getBody();
            
            if (body instanceof StreamingResponseBody) {
                StreamingResponseBody original = (StreamingResponseBody) body;
                TracingStreamingResponseBodyWrapper wrapper = new TracingStreamingResponseBodyWrapper(original, contextCopy);
                
                // Create new ResponseEntity with wrapped body
                ResponseEntity<StreamingResponseBody> newResponseEntity = ResponseEntity
                    .status(responseEntity.getStatusCode())
                    .headers(responseEntity.getHeaders())
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
class TracingStreamingResponseBodyWrapper implements StreamingResponseBody {
    
    private final StreamingResponseBody delegate;
    private final ITraceContext traceContext;
    
    public TracingStreamingResponseBodyWrapper(StreamingResponseBody delegate, ITraceContext traceContext) {
        this.delegate = delegate;
        this.traceContext = traceContext;
    }
    
    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        if (traceContext == null) {
            delegate.writeTo(outputStream);
            return;
        }

        ITraceSpan streamingSpan = traceContext.newSpan()
                                               .method(this.delegate.getClass(), "writeTo")
                                               .start();

        TraceContextHolder.attach(traceContext);
        try {
            delegate.writeTo(outputStream);
        } catch (Exception e) {
            streamingSpan.tag(e);
            throw e;
        } finally {
            streamingSpan.finish();

            TraceContextHolder.detach();
        }
    }
}
