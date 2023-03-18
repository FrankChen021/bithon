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

package org.bithon.agent.plugin.alibaba.druid.interceptor;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.plugin.alibaba.druid.metric.MonitoredSourceManager;

/**
 * @author frankchen
 */
public class DruidDataSourceInit extends AbstractInterceptor {

    @Override
    public InterceptionDecision before(AopContext aopContext) {
        IBithonObject obj = aopContext.getTargetAs();
        Boolean initialized = (Boolean) obj.getInjectedObject();
        if (initialized != null && initialized) {
            return InterceptionDecision.SKIP_LEAVE;
        }

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void after(AopContext aopContext) {
        if (aopContext.hasException()) {
            return;
        }

        IBithonObject obj = aopContext.getTargetAs();
        boolean initialized = MonitoredSourceManager.getInstance().addDataSource(aopContext.getTargetAs());
        obj.setInjectedObject(initialized);
    }
}
