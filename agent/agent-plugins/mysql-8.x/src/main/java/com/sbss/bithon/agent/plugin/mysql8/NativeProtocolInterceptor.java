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

package com.sbss.bithon.agent.plugin.mysql8;


import com.mysql.cj.conf.HostInfo;
import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.sql.SqlCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.sql.SqlMetricCollector;
import com.sbss.bithon.agent.core.utils.MiscUtils;
import com.sbss.bithon.agent.core.utils.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * @author frankchen
 */
public class NativeProtocolInterceptor extends AbstractInterceptor {

    SqlMetricCollector sqlMetricCollector;

    @Override
    public boolean initialize() throws Exception {
        sqlMetricCollector = MetricCollectorManager.getInstance()
                                                   .getOrRegister("mysql8-metrics", SqlMetricCollector.class);

        return super.initialize();
    }

    @Override
    public void onMethodLeave(AopContext aopContext) throws Exception {

        String methodName = aopContext.getMethod().getName();
        Object nativeProtocol = aopContext.getTarget();

        Object session = ReflectionUtils.getFieldValue(nativeProtocol, "session");
        HostInfo hostInfo = (HostInfo) ReflectionUtils.getFieldValue(session, "hostInfo");

        SqlCompositeMetric metric = sqlMetricCollector.getOrCreateMetric(MiscUtils.cleanupConnectionString(hostInfo.getDatabaseUrl()));

        if (MySql8Plugin.METHOD_SEND_COMMAND.equals(methodName)) {
            Object message = aopContext.getArgs()[0];
            Method getPositionMethod = message.getClass().getDeclaredMethod("getPosition", null);
            Integer position = (Integer) getPositionMethod.invoke(message);
            metric.updateBytesOut(position);
        } else {
            //TODO： HOW TO ???
        }
    }
}
