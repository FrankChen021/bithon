package com.sbss.bithon.agent.plugin.mysql8;

import com.mysql.cj.conf.HostInfo;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.IMetricCollector;
import com.sbss.bithon.agent.core.metric.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.sql.SqlMetricSet;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.utils.MiscUtils;
import com.sbss.bithon.agent.core.utils.ReflectionUtils;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frankchen
 */
public class SqlMetricCollector implements IMetricCollector {
    static final SqlMetricCollector INSTANCE = new SqlMetricCollector();
    private static final Logger log = LoggerFactory.getLogger(SqlMetricCollector.class);
    private static final String MYSQL_COUNTER_NAME = "mysql8";
    private static final String DRIVER_TYPE_MYSQL = "mysql";
    private final Map<String, SqlMetricSet> metricMap = new ConcurrentHashMap<>();

    private SqlMetricCollector() {
        try {
            MetricCollectorManager.getInstance().register(MYSQL_COUNTER_NAME, this);
        } catch (Exception e) {
            log.error("druid counter init failed due to ", e);
        }
    }

    static SqlMetricCollector getInstance() {
        return INSTANCE;
    }

    public void update(AopContext aopContext) {
        long costTime = aopContext.getCostTime();

        if (aopContext.getTarget() instanceof Statement) {
            recordExecution(aopContext, costTime);
        } else {
            recordBytes(aopContext);
        }
    }

    private void recordBytes(AopContext aopContext) {

        try {
            String methodName = aopContext.getMethod().getName();
            Object nativeProtocol = aopContext.getTarget();

            Object session = ReflectionUtils.getFieldValue(nativeProtocol, "session");
            HostInfo hostInfo = (HostInfo) ReflectionUtils.getFieldValue(session, "hostInfo");

            SqlMetricSet mysqlMetricSetStorage = metricMap.computeIfAbsent(MiscUtils.cleanupConnectionString(hostInfo.getDatabaseUrl()),
                                                                           k -> new SqlMetricSet(k, DRIVER_TYPE_MYSQL));

            if (MySql8Plugin.METHOD_SEND_COMMAND.equals(methodName)) {
                Object message = aopContext.getArgs()[0];
                Method getPositionMethod = message.getClass().getDeclaredMethod("getPosition", null);
                Integer position = (Integer) getPositionMethod.invoke(message);
                mysqlMetricSetStorage.addBytesOut(position);
            } else {
                //TODO： 暂时无方法统计
            }
        } catch (Exception e) {
            log.error("get bytes info error", e);
        }
    }

    private void recordExecution(AopContext aopContext, long costTime) {
        String methodName = aopContext.getMethod().getName();

        Statement statement = (Statement) aopContext.getTarget();
        try {
            String connectionString = MiscUtils.cleanupConnectionString(statement.getConnection()
                                                                                 .getMetaData()
                                                                                 .getURL());

            boolean isQuery = true;
            boolean failed = false;

            SqlMetricSet metric = metricMap.computeIfAbsent(connectionString, k -> new SqlMetricSet(k, "mysql"));

            if (MySql8Plugin.METHOD_EXECUTE_UPDATE.equals(methodName)
                || MySql8Plugin.METHOD_EXECUTE_UPDATE_INTERNAL.equals(methodName)) {
                isQuery = false;
            } else if ((MySql8Plugin.METHOD_EXECUTE.equals(methodName) || MySql8Plugin.METHOD_EXECUTE_INTERNAL.equals(
                methodName))) {
                Object result = aopContext.castReturningAs();
                if (result instanceof Boolean && !(boolean) result) {
                    isQuery = false;
                }
            }

            if (null != aopContext.getException()) {
                failed = true;
            }

            metric.add(isQuery, failed, costTime);
        } catch (SQLException e) {
            log.error("unknown or unreachable connection intercepted by agent! this data may not been recorded!", e);
        }
    }

    @Override
    public boolean isEmpty() {
        return metricMap.isEmpty();
    }

    @Override
    public List<Object> collect(IMessageConverter messageConverter,
                                int interval,
                                long timestamp) {
        List<Object> messages = new ArrayList<>();
        for (Map.Entry<String, SqlMetricSet> entry : metricMap.entrySet()) {
            Object message = messageConverter.from(timestamp, interval, entry.getValue());
            if (message != null) {
                messages.add(message);
            }
        }
        return messages;
    }
}
