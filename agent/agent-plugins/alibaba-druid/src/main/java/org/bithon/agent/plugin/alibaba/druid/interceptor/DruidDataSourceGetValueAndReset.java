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

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceStatValue;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.core.metric.domain.jdbc.JdbcPoolMetrics;
import org.bithon.agent.plugin.alibaba.druid.metric.MonitoredSource;
import org.bithon.agent.plugin.alibaba.druid.metric.MonitoredSourceManager;

/**
 * @author frankchen
 */
public class DruidDataSourceGetValueAndReset extends AbstractInterceptor {
    @Override
    public void onMethodLeave(AopContext aopContext) {
        updateMetrics(aopContext.castTargetAs(),
                      aopContext.castReturningAs());
    }

    private void updateMetrics(DruidDataSource dataSource, DruidDataSourceStatValue statistic) {
        if (statistic == null) {
            return;
        }
        MonitoredSource source = MonitoredSourceManager.getInstance().getMonitoredDataSource(dataSource);
        if (source == null) {
            return;
        }

        JdbcPoolMetrics metrics = source.getJdbcMetric();
        metrics.createCount.update(statistic.getPhysicalConnectCount());
        metrics.destroyCount.update(statistic.getPhysicalCloseCount());
        metrics.createErrorCount.update(statistic.getPhysicalConnectErrorCount());
        metrics.logicConnectionCount.update(statistic.getConnectCount());
        metrics.logicCloseCount.update(statistic.getCloseCount());
        metrics.executeCount.update(statistic.getExecuteCount());
        metrics.commitCount.update(statistic.getCommitCount());
        metrics.rollbackCount.update(statistic.getRollbackCount());
        metrics.startTransactionCount.update(statistic.getStartTransactionCount());
        metrics.waitThreadCount.update(statistic.getWaitThreadCount());
    }
}
