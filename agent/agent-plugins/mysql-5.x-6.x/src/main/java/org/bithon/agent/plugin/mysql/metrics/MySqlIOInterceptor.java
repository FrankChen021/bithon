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

package org.bithon.agent.plugin.mysql.metrics;

import com.mysql.jdbc.Buffer;
import com.mysql.jdbc.MySQLConnection;
import com.mysql.jdbc.MysqlIO;
import com.mysql.jdbc.ResultSetImpl;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.metric.domain.sql.SQLMetrics;
import org.bithon.agent.observability.metric.domain.sql.SqlMetricRegistry;
import org.bithon.agent.observability.utils.MiscUtils;
import org.bithon.agent.plugin.mysql.MySqlPlugin;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.sql.SQLException;

/**
 * MySQL IO
 *
 * @author frankchen
 */
public class MySqlIOInterceptor extends AfterInterceptor {

    private final SqlMetricRegistry metricRegistry = SqlMetricRegistry.get();

    @Override
    public void after(AopContext aopContext) throws SQLException {
        String methodName = aopContext.getMethod();

        MysqlIO mysqlIO = aopContext.getTargetAs();

        MySQLConnection connection = (MySQLConnection) ReflectionUtils.getFieldValue(mysqlIO, "connection");

        SQLMetrics metric = metricRegistry.getOrCreateMetrics(MiscUtils.cleanupConnectionString(connection.getURL()));

        if (MySqlPlugin.METHOD_SEND_COMMAND.equals(methodName)) {
            Buffer queryPacket = (Buffer) aopContext.getArgs()[2];
            if (queryPacket != null) {
                metric.updateBytesOut(queryPacket.getPosition());
            }
        } else {
            ResultSetImpl resultSet = aopContext.getReturningAs();
            int bytesIn = resultSet.getBytesSize();
            if (bytesIn > 0) {
                metric.updateBytesIn(bytesIn);
            }
        }
    }
}
