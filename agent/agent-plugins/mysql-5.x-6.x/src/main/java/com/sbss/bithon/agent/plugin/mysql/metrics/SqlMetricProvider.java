package com.sbss.bithon.agent.plugin.mysql.metrics;

import com.mysql.jdbc.Buffer;
import com.mysql.jdbc.MysqlIO;
import com.mysql.jdbc.ResultSetImpl;
import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metrics.IMetricProvider;
import com.sbss.bithon.agent.core.metrics.MetricProviderManager;
import com.sbss.bithon.agent.core.metrics.sql.SqlMetric;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.utils.ReflectionUtils;
import com.sbss.bithon.agent.plugin.mysql.MySqlPlugin;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frankchen
 */
public class SqlMetricProvider implements IMetricProvider {
    static final SqlMetricProvider INSTANCE = new SqlMetricProvider();
    private static final Logger log = LoggerFactory.getLogger(SqlMetricProvider.class);
    private static final String MYSQL_COUNTER_NAME = "mysql";
    private static final String DRIVER_TYPE_MYSQL = "mysql";
    private final Map<String, SqlMetric> metricMap;

    private SqlMetricProvider() {
        metricMap = new ConcurrentHashMap<>();

        try {
            MetricProviderManager.getInstance().register(MYSQL_COUNTER_NAME, this);
        } catch (Exception e) {
            log.error("druid counter init failed due to ", e);
        }
    }

    static SqlMetricProvider getInstance() {
        return INSTANCE;
    }

    public void recordIO(AopContext aopContext) {
        String methodName = aopContext.getMethod().getName();
        try {
            MysqlIO mysqlIO = (MysqlIO) aopContext.getTarget();

            String host = (String) ReflectionUtils.getFieldValue(mysqlIO, "host");
            Integer port = (Integer) ReflectionUtils.getFieldValue(mysqlIO, "port");
            String hostPort = host + ":" + port;

            // 尝试记录新的mysql连接
            SqlMetric counter = metricMap.computeIfAbsent(hostPort,
                                                          k -> new SqlMetric(k, DRIVER_TYPE_MYSQL));

            if (MySqlPlugin.METHOD_SEND_COMMAND.equals(methodName)) {
                Buffer queryPacket = (Buffer) aopContext.getArgs()[2];
                if (queryPacket != null) {
                    counter.addBytesOut(queryPacket.getPosition());
                }
            } else {
                ResultSetImpl resultSet = aopContext.castReturningAs();
                int bytesIn = resultSet.getBytesSize();
                if (bytesIn > 0) {
                    counter.addBytesIn(resultSet.getBytesSize());
                }
            }
        } catch (Exception e) {
            log.error("get bytes info error", e);
        }
    }

    public void recordExecution(AopContext aopContext, String hostAndPort) {
        if (hostAndPort == null) {
            log.warn("hostAndPort is null");
            return;
        }
        String methodName = aopContext.getMethod().getName();

        boolean isQuery = true;
        if (MySqlPlugin.METHOD_EXECUTE_UPDATE.equals(methodName) ||
            MySqlPlugin.METHOD_EXECUTE_UPDATE_INTERNAL.equals(methodName)) {
            isQuery = false;
        } else if ((MySqlPlugin.METHOD_EXECUTE.equals(methodName) ||
                    MySqlPlugin.METHOD_EXECUTE_INTERNAL.equals(methodName))) {
            Object result = aopContext.castReturningAs();
            if (result instanceof Boolean && !(boolean) result) {
                isQuery = false;
            }
        }

        metricMap.computeIfAbsent(hostAndPort,
                                  k -> new SqlMetric(k, DRIVER_TYPE_MYSQL))
                 .add(isQuery,
                      aopContext.getException() != null,
                      aopContext.getCostTime());
    }

    @Override
    public boolean isEmpty() {
        return metricMap.isEmpty();
    }

    @Override
    public List<Object> buildMessages(IMessageConverter messageConverter,
                                      AppInstance appInstance,
                                      int interval,
                                      long timestamp) {
        List<Object> messages = new ArrayList<>();
        for (Map.Entry<String, SqlMetric> entry : metricMap.entrySet()) {
            Object message = messageConverter.from(appInstance, timestamp, interval, entry.getValue());
            if (message != null) {
                messages.add(message);
            }
        }
        return messages;
    }
}
