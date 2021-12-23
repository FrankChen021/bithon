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

package org.bithon.agent.plugin.log4j2.interceptor;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.spi.StandardLevel;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.dispatcher.Dispatcher;
import org.bithon.agent.core.dispatcher.Dispatchers;
import org.bithon.agent.core.event.EventMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * @author frankchen
 */
public class LoggerLogMessage extends AbstractInterceptor {

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        Level logLevel = (Level) aopContext.getArgs()[1];
        Throwable e = (Throwable) aopContext.getArgs()[4];
        return e != null && StandardLevel.ERROR.equals(logLevel.getStandardLevel()) ?
               InterceptionDecision.CONTINUE : InterceptionDecision.SKIP_LEAVE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        Throwable exception = (Throwable) aopContext.getArgs()[4];

        Map<String, Object> exceptionArgs = new HashMap<>();
        exceptionArgs.put("exceptionClass", exception.getClass().getName());
        exceptionArgs.put("message", exception.getMessage() == null ? "" : exception.getMessage());
        exceptionArgs.put("stack", exception.toString());
        EventMessage exceptionEvent = new EventMessage("exception", exceptionArgs);
        Dispatcher dispatcher = Dispatchers.getOrCreate(Dispatchers.DISPATCHER_NAME_EVENT);
        dispatcher.sendMessage(dispatcher.getMessageConverter().from(exceptionEvent));
    }
}
