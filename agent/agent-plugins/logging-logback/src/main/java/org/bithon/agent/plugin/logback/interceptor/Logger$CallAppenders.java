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

package org.bithon.agent.plugin.logback.interceptor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.interceptor.BeforeInterceptor;
import org.bithon.agent.observability.dispatcher.Dispatcher;
import org.bithon.agent.observability.dispatcher.Dispatchers;
import org.bithon.agent.observability.event.EventMessage;
import org.bithon.agent.observability.tracing.context.ITraceContext;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.bithon.agent.observability.tracing.context.propagation.TraceMode;

import java.util.HashMap;
import java.util.Map;

/**
 * @author frankchen
 */
public class Logger$CallAppenders extends BeforeInterceptor {

    @Override
    public void before(AopContext aopContext) {
        ILoggingEvent iLoggingEvent = (ILoggingEvent) aopContext.getArgs()[0];
        if (iLoggingEvent.getLevel().toInt() != Level.ERROR.toInt()) {
            return;
        }
        IThrowableProxy exception = iLoggingEvent.getThrowableProxy();
        if (null == exception) {
            return;
        }

        Map<String, Object> exceptionArgs = new HashMap<>();
        exceptionArgs.put("exceptionClass", exception.getClassName());
        exceptionArgs.put("message", exception.getMessage() == null ? "" : exception.getMessage());
        exceptionArgs.put("stack", StackTraceBuilder.build(exception));
        exceptionArgs.put("thread", Thread.currentThread().getName());
        ITraceContext traceContext = TraceContextHolder.current();
        if (traceContext != null && traceContext.traceMode().equals(TraceMode.TRACE)) {
            exceptionArgs.put("traceId", traceContext.traceId());
        }

        EventMessage exceptionEvent = new EventMessage("exception", exceptionArgs);
        Dispatcher dispatcher = Dispatchers.getOrCreate(Dispatchers.DISPATCHER_NAME_EVENT);
        dispatcher.sendMessage(dispatcher.getMessageConverter().from(exceptionEvent));
    }

    static class StackTraceBuilder {
        public static String build(IThrowableProxy exception) {
            final StringBuilder stackTrace = new StringBuilder(2048);

            build(stackTrace, false, exception);

            IThrowableProxy cause = exception.getCause();
            while (cause != null) {
                build(stackTrace, true, cause);
                cause = cause.getCause();
            }

            return stackTrace.toString();
        }

        private static void build(StringBuilder stackTrace, boolean isCausedBy, IThrowableProxy exception) {
            if (isCausedBy) {
                stackTrace.append("Caused By: ");
            }
            stackTrace.append(exception.getClassName());
            stackTrace.append(':');
            stackTrace.append(exception.getMessage());
            stackTrace.append('\n');
            for (StackTraceElementProxy p : exception.getStackTraceElementProxyArray()) {
                stackTrace.append('\t');
                stackTrace.append(p.getSTEAsString());
                stackTrace.append("\n");
            }
        }
    }
}
