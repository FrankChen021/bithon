package com.sbss.bithon.agent.plugin.alibaba.druid.interceptor;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.InterceptionDecision;
import com.sbss.bithon.agent.plugin.alibaba.druid.metric.MonitoredSourceManager;
import com.sbss.bithon.agent.plugin.alibaba.druid.metric.DruidSqlMetricProvider;

import java.sql.Statement;

/**
 * @author frankchen
 */
public class DruidSqlInterceptor extends AbstractInterceptor {

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
        Statement statement = aopContext.castTargetAs();

        aopContext.setUserContext(MonitoredSourceManager.parseDataSourceUri(statement.getConnection().getMetaData().getURL()));

        return InterceptionDecision.CONTINUE;
    }

    @Override
    public void onMethodLeave(AopContext aopContext) {
        String dataSourceUri = aopContext.castUserContextAs();
        if (dataSourceUri != null) {
            DruidSqlMetricProvider.getInstance().update(aopContext.getMethod().getName(),
                                                        dataSourceUri,
                                                        aopContext,
                                                        aopContext.getCostTime());
        }
    }
}
