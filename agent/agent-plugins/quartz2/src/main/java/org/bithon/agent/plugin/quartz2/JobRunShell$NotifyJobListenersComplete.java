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

package org.bithon.agent.plugin.quartz2;

import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.interceptor.BeforeInterceptor;
import org.bithon.agent.observability.dispatcher.Dispatcher;
import org.bithon.agent.observability.dispatcher.Dispatchers;
import org.bithon.agent.observability.event.EventMessage;
import org.bithon.agent.observability.tracing.context.TraceContextHolder;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/8/6 21:56
 */
public class JobRunShell$NotifyJobListenersComplete extends BeforeInterceptor {

    private Dispatcher dispatcher = Dispatchers.getOrCreate(Dispatchers.DISPATCHER_NAME_EVENT);

    @Override
    public void before(AopContext aopContext) {
        JobExecutionContext jec = aopContext.getArgAs(0);
        JobExecutionException exception = aopContext.getArgAs(1);

        // log
        {
            boolean isExceptionOccurred = exception != null;
            Map<String, Object> quartzLog = new HashMap<>(8);
            quartzLog.put("timestamp", aopContext.getStartTimestamp());

            quartzLog.put("job", jec.getJobDetail().getKey().toString());
            quartzLog.put("class", jec.getJobDetail().getJobClass().getName());
            quartzLog.put("exception", isExceptionOccurred ? exception.getCause().toString() : "");
            quartzLog.put("traceId", TraceContextHolder.currentTraceId());

            quartzLog.put("duration", jec.getJobRunTime());
            quartzLog.put("successfulCount", isExceptionOccurred ? 0 : 1);
            quartzLog.put("exceptionCount", isExceptionOccurred ? 1 : 0);
            dispatcher.sendMessage(dispatcher.getMessageConverter().from(new EventMessage("quartz", quartzLog)));
        }

        // save the object on current target,
        // so that JobRunShell$Run can get the exception
        IBithonObject bithonObject = aopContext.getTargetAs();
        bithonObject.setInjectedObject(exception);
    }
}
