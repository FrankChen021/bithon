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

package com.sbss.bithon.agent.plugin.undertow.interceptor;

import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.utils.ReflectionUtils;
import com.sbss.bithon.agent.plugin.undertow.metric.WebServerMetricCollector;
import io.undertow.Undertow;
import org.xnio.XnioWorker;

import java.util.List;

/**
 * @author frankchen
 */
public class UndertowStart extends AbstractInterceptor {

    private Integer port;

    @Override
    public void onMethodLeave(AopContext aopContext) {
        if (port == null && !aopContext.hasException()) {
            Undertow server = (Undertow) aopContext.getTarget();

            List<?> listeners = (List<?>) ReflectionUtils.getFieldValue(server, "listeners");
            XnioWorker worker = (XnioWorker) ReflectionUtils.getFieldValue(server, "worker");
            port = Integer.parseInt(ReflectionUtils.getFieldValue(listeners.iterator().next(), "port").toString());
            AgentContext.getInstance().getAppInstance().setPort(port);

            Object taskPool = ReflectionUtils.getFieldValue(worker, "taskPool");
            WebServerMetricCollector.getInstance().setThreadPool(taskPool);
        }
    }
}
