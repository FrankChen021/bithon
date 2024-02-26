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

package org.bithon.agent.plugin.httpclient.jetty;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.bithon.agent.observability.metric.domain.http.HttpOutgoingMetricsRegistry;
import org.bithon.agent.observability.tracing.config.TraceConfig;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.context.TraceContextFactory;
import org.bithon.component.commons.tracing.SpanKind;
import org.bithon.component.commons.tracing.Tags;
import org.eclipse.jetty.client.HttpRequest;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.util.Callback;

import java.nio.ByteBuffer;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/5/13 7:56 下午
 */
public class HttpRequest$Send extends BeforeInterceptor {

    private final HttpOutgoingMetricsRegistry metricRegistry = HttpOutgoingMetricsRegistry.get();

    private final TraceConfig traceConfig = ConfigurationManager.getInstance().getConfig(TraceConfig.class);

    /**
     * {@link org.eclipse.jetty.client.HttpRequest#send(Response.CompleteListener)}
     */
    @Override
    public void before(AopContext aopContext) {
        HttpRequest httpRequest = aopContext.getTargetAs();

        final ITraceSpan span = TraceContextFactory.newAsyncSpan("http-client");
        if (span != null) {
            span.method(aopContext.getTargetClass(), aopContext.getMethod())
                .kind(SpanKind.CLIENT)
                .tag(Tags.Http.CLIENT, "jetty")
                .tag(Tags.Http.URL, httpRequest.getURI().toString())
                .tag(Tags.Http.METHOD, httpRequest.getMethod())
                .propagate(httpRequest.getHeaders(), HttpFields::put)
                .start();
        }

        final long startAt = System.nanoTime();

        // replace listener to record metrics
        final Object rawListener = aopContext.getArgs()[0];
        aopContext.getArgs()[0] = new Response.Listener() {
            @Override
            public void onSuccess(Response response) {
                if (rawListener instanceof Response.SuccessListener) {
                    ((Response.SuccessListener) rawListener).onSuccess(response);
                }
            }

            @Override
            public void onHeaders(Response response) {
                if (rawListener instanceof Response.HeadersListener) {
                    ((Response.HeadersListener) rawListener).onHeaders(response);
                }
            }

            @Override
            public boolean onHeader(Response response, HttpField httpField) {
                if (rawListener instanceof Response.HeaderListener) {
                    return ((Response.HeaderListener) rawListener).onHeader(response, httpField);
                }
                return true;
            }

            @Override
            public void onFailure(Response response, Throwable throwable) {
                if (rawListener instanceof Response.FailureListener) {
                    ((Response.FailureListener) rawListener).onFailure(response, throwable);
                }
            }

            @Override
            public void onContent(Response response, ByteBuffer byteBuffer) {
                if (rawListener instanceof Response.ContentListener) {
                    ((Response.ContentListener) rawListener).onContent(response, byteBuffer);
                }
            }

            @Override
            public void onComplete(Result result) {
                //
                // metrics
                //
                if (result.isFailed()) {
                    metricRegistry.addExceptionRequest(result.getRequest().getURI().toString(),
                                                       result.getRequest().getMethod(),
                                                       System.nanoTime() - startAt);
                } else {
                    metricRegistry.addRequest(result.getRequest().getURI().toString(),
                                              result.getRequest().getMethod(),
                                              result.getResponse().getStatus(),
                                              System.nanoTime() - startAt);
                }

                //
                // trace
                //
                try {
                    if (span != null) {
                        span.tag(result.getFailure())
                            .tag(Tags.Http.STATUS, String.valueOf(result.getResponse().getStatus()))
                            .configIfTrue(!traceConfig.getHeaders().getResponse().isEmpty(),
                                          (s) -> {
                                              for (String name : traceConfig.getHeaders().getResponse()) {
                                                  String val = result.getResponse().getHeaders().get(name);
                                                  if (val != null) {
                                                      s.tag(Tags.Http.RESPONSE_HEADER_PREFIX + name, val);
                                                  }
                                              }
                                          })
                            .finish();
                        span.context().finish();
                    }
                } catch (Throwable ignored) {
                }

                if (rawListener instanceof Response.CompleteListener) {
                    ((Response.CompleteListener) rawListener).onComplete(result);
                }
            }

            @Override
            public void onBegin(Response response) {
                if (rawListener instanceof Response.BeginListener) {
                    ((Response.BeginListener) rawListener).onBegin(response);
                }
            }

            @Override
            public void onContent(Response response, ByteBuffer byteBuffer, Callback callback) {
                if (rawListener instanceof Response.AsyncContentListener) {
                    ((Response.AsyncContentListener) rawListener).onContent(response, byteBuffer, callback);
                }
            }
        };
    }
}
