package com.sbss.bithon.agent.plugin.mysql.metrics;

import com.mysql.jdbc.Buffer;
import com.mysql.jdbc.MysqlIO;
import com.mysql.jdbc.ResultSetImpl;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metric.IMetricCollector;
import com.sbss.bithon.agent.core.metric.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.sql.SqlMetricSet;
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
public class SqlMetricCollector implements IMetricCollector {
    static final SqlMetricCollector INSTANCE = new SqlMetricCollector();
    private static final Logger log = LoggerFactory.getLogger(SqlMetricCollector.class);
    private static final String MYSQL_COUNTER_NAME = "mysql";
    private static final String DRIVER_TYPE_MYSQL = "mysql";
    private final Map<String, SqlMetricSet> metricMap;

    private SqlMetricCollector() {
        metricMap = new ConcurrentHashMap<>();

        try {
            MetricCollectorManager.getInstance().register(MYSQL_COUNTER_NAME, this);
        } catch (Exception e) {
            log.error("druid counter init failed due to ", e);
        }
    }

    static SqlMetricCollector getInstance() {
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
            SqlMetricSet counter = metricMap.computeIfAbsent(hostPort,
                                                          k -> new SqlMetricSet(k, DRIVER_TYPE_MYSQL));

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
                                  k -> new SqlMetricSet(k, DRIVER_TYPE_MYSQL))
                 .add(isQuery,
                      aopContext.getException() != null,
                      aopContext.getCostTime());
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
