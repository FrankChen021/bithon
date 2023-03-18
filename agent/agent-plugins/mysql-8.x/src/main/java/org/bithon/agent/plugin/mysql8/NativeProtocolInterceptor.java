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

package org.bithon.agent.plugin.mysql8;


import com.mysql.cj.conf.HostInfo;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.observability.metric.domain.sql.SQLMetrics;
import org.bithon.agent.observability.metric.domain.sql.SqlMetricRegistry;
import org.bithon.agent.observability.utils.MiscUtils;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.lang.reflect.Method;

/**
 * @author frankchen
 */
public class NativeProtocolInterceptor extends AbstractInterceptor {

    private final SqlMetricRegistry metricRegistry = SqlMetricRegistry.get();

    @Override
    public void after(AopContext aopContext) throws Exception {

        String methodName = aopContext.getMethod().getName();
        Object nativeProtocol = aopContext.getTarget();

        Object session = ReflectionUtils.getFieldValue(nativeProtocol, "session");
        HostInfo hostInfo = (HostInfo) ReflectionUtils.getFieldValue(session, "hostInfo");

        SQLMetrics metric = metricRegistry.getOrCreateMetrics(MiscUtils.cleanupConnectionString(hostInfo.getDatabaseUrl()));

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
