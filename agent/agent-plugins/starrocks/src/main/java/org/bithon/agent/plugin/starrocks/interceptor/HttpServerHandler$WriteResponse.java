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

package org.bithon.agent.plugin.starrocks.interceptor;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.context.InterceptorContext;
import org.bithon.agent.observability.metric.domain.web.HttpIncomingMetricsRegistry;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.component.commons.utils.ReflectionUtils;

public class HttpServerHandler$WriteResponse extends AfterInterceptor
{
  private final HttpIncomingMetricsRegistry metricRegistry = HttpIncomingMetricsRegistry.get();

  @Override
  public void after(AopContext aopContext) throws Exception
  {
    InterceptorContext.remove(InterceptorContext.KEY_URI);

    ITraceContext traceContext = TraceContextHolder.current();
    try {
      traceContext.currentSpan()
                  .tag(aopContext.getException())
                  .finish();

      HttpRequest request = (HttpRequest) ReflectionUtils.getFieldValue(aopContext.getArgs()[0], "request");
      HttpResponseStatus status = (HttpResponseStatus) aopContext.getArgs()[2];
      int httpStatus = status.code();
      int count4xx = httpStatus >= 400 && httpStatus < 500 ? 1 : 0;
      int count5xx = httpStatus >= 500 ? 1 : 0;

      this.metricRegistry.getOrCreateMetrics("", request.method().toString(), request.uri(), httpStatus)
                         .updateRequest(aopContext.getExecutionTime(), count4xx, count5xx);
    }
    finally {
      traceContext.finish();
      try {
        TraceContextHolder.remove();
      }
      catch (Exception ignored) {
      }
    }
  }
}

