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

package com.sbss.bithon.agent.plugin.mysql.metrics;

import com.mysql.jdbc.Buffer;
import com.mysql.jdbc.MySQLConnection;
import com.mysql.jdbc.MysqlIO;
import com.mysql.jdbc.ResultSetImpl;
import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.sql.SqlCompositeMetric;
import com.sbss.bithon.agent.core.metric.domain.sql.SqlMetricCollector;
import com.sbss.bithon.agent.core.utils.MiscUtils;
import com.sbss.bithon.agent.core.utils.ReflectionUtils;
import com.sbss.bithon.agent.plugin.mysql.MySqlPlugin;

import java.sql.SQLException;

/**
 * MySQL IO
 *
 * @author frankchen
 */
public class MysqlIOInterceptor extends AbstractInterceptor {

    SqlMetricCollector metricCollector;

    @Override
    public boolean initialize() throws Exception {
        metricCollector = MetricCollectorManager.getInstance().getOrRegister("mysql-metrics", SqlMetricCollector.class);
        return super.initialize();
    }

    @Override
    public void onMethodLeave(AopContext aopContext) throws SQLException {
        String methodName = aopContext.getMethod().getName();

        MysqlIO mysqlIO = aopContext.castTargetAs();

        MySQLConnection connection = (MySQLConnection) ReflectionUtils.getFieldValue(mysqlIO, "connection");

        SqlCompositeMetric metric = metricCollector.getOrCreateMetric(MiscUtils.cleanupConnectionString(connection.getURL()));

        if (MySqlPlugin.METHOD_SEND_COMMAND.equals(methodName)) {
            Buffer queryPacket = (Buffer) aopContext.getArgs()[2];
            if (queryPacket != null) {
                metric.updateBytesOut(queryPacket.getPosition());
            }
        } else {
            ResultSetImpl resultSet = aopContext.castReturningAs();
            int bytesIn = resultSet.getBytesSize();
            if (bytesIn > 0) {
                metric.updateBytesIn(bytesIn);
            }
        }
    }
}
