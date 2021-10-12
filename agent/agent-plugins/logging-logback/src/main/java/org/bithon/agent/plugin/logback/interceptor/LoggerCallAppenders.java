/*
 *    Copyright 2020 bithon.cn
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
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.core.context.InterceptorContext;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;

/**
 * @author frankchen
 */
public class LoggerCallAppenders extends AbstractInterceptor {
    private LogMetricCollector counter;

    @Override
    public void onMethodLeave(AopContext context) {
        ILoggingEvent iLoggingEvent = (ILoggingEvent) context.getArgs()[0];
        if (iLoggingEvent.getLevel().toInt() != Level.ERROR.toInt()) {
            return;
        }
        IThrowableProxy exception = iLoggingEvent.getThrowableProxy();
        if (null == exception) {
            return;
        }

        if (counter == null) {
            counter = MetricCollectorManager.getInstance().getOrRegister("logback", LogMetricCollector.class);
        }

        counter.addException((String) InterceptorContext.get("uri"),
                             exception);
    }
}
