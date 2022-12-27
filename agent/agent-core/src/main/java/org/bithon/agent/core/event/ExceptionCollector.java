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

package org.bithon.agent.core.event;

import org.bithon.agent.core.dispatcher.Dispatcher;
import org.bithon.agent.core.dispatcher.Dispatchers;
import org.bithon.agent.core.tracing.context.ITraceContext;
import org.bithon.agent.core.tracing.context.TraceContextHolder;
import org.bithon.agent.core.tracing.propagation.TraceMode;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 27/12/22 2:29 pm
 */
public class ExceptionCollector {

    public static void collect(Throwable throwable) {
        collect(throwable, null);
    }
    public static void collect(Throwable throwable, Map<String, String> extraArgs) {
        StringWriter stackTrace = new StringWriter(512);
        throwable.printStackTrace(new PrintWriter(stackTrace));

        Map<String, Object> exceptionArgs = new HashMap<>(extraArgs == null ? Collections.emptyMap() : extraArgs);
        exceptionArgs.put("exceptionClass", throwable.getClass().getName());
        exceptionArgs.put("message", throwable.getMessage() == null ? "" : throwable.getMessage());
        exceptionArgs.put("stack", stackTrace.toString());
        exceptionArgs.put("thread", Thread.currentThread().getName());
        ITraceContext traceContext = TraceContextHolder.current();
        if (traceContext != null && traceContext.traceMode().equals(TraceMode.TRACE)) {
            exceptionArgs.put("traceId", traceContext.traceId());
        }


        EventMessage exceptionEvent = new EventMessage("exception", exceptionArgs);
        Dispatcher dispatcher = Dispatchers.getOrCreate(Dispatchers.DISPATCHER_NAME_EVENT);
        dispatcher.sendMessage(dispatcher.getMessageConverter().from(exceptionEvent));
    }
}
