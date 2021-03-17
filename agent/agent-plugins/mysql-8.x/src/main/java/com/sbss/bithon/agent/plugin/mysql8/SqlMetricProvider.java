package com.sbss.bithon.agent.plugin.mysql8;

import com.sbss.bithon.agent.core.context.AppInstance;
import com.sbss.bithon.agent.core.dispatcher.IMessageConverter;
import com.sbss.bithon.agent.core.metrics.IMetricProvider;
import com.sbss.bithon.agent.core.metrics.MetricProviderManager;
import com.sbss.bithon.agent.core.metrics.sql.SqlMetric;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.utils.ReflectionUtils;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.sql.Statement;
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
    private static final String MYSQL_COUNTER_NAME = "mysql8";
    private static final String DRIVER_TYPE_MYSQL = "mysql";
    private final Map<String, SqlMetric> counters = new ConcurrentHashMap<>();

    private SqlMetricProvider() {
        try {
            MetricProviderManager.getInstance().register(MYSQL_COUNTER_NAME, this);
        } catch (Exception e) {
            log.error("druid counter init failed due to ", e);
        }
    }

    static SqlMetricProvider getInstance() {
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
            Object hostInfo = ReflectionUtils.getFieldValue(session, "hostInfo");

            // 反射式的获取当前mysqlIO host+port
            String host = (String) ReflectionUtils.getFieldValue(hostInfo, "host");
            Integer port = (Integer) ReflectionUtils.getFieldValue(hostInfo, "port");
            String hostPort = host + ":" + port;

            // 尝试记录新的mysql连接
            SqlMetric mysqlMetricStorage = counters.computeIfAbsent(hostPort, k -> new SqlMetric(k, DRIVER_TYPE_MYSQL));

            if (MySql8Plugin.METHOD_SEND_COMMAND.equals(methodName)) {
                Object message = aopContext.getArgs()[0];
                Method getPositionMethod = message.getClass().getDeclaredMethod("getPosition", null);
                Integer position = (Integer) getPositionMethod.invoke(message);
                mysqlMetricStorage.addBytesOut(position);
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
            String hostPort = parseDBAddress(statement.getConnection().getMetaData().getURL());

            boolean isQuery = true;
            boolean failed = false;

            // 尝试记录新的mysql连接
            SqlMetric mysqlMetricStorage = counters.computeIfAbsent(hostPort, k -> new SqlMetric(k, "mysql"));

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

            mysqlMetricStorage.add(isQuery, failed, costTime);
        } catch (SQLException e) {
            log.error("unknown or unreachable connection intercepted by agent! this data may not been recorded!", e);
        } catch (URISyntaxException e) {
            log.error("db url parse failed!", e);
        }
    }

    @Override
    public boolean isEmpty() {
        return counters.isEmpty();
    }

    @Override
    public List<Object> buildMessages(IMessageConverter messageConverter,
                                      AppInstance appInstance,
                                      int interval,
                                      long timestamp) {
        List<Object> messages = new ArrayList<>();
        for (Map.Entry<String, SqlMetric> entry : counters.entrySet()) {
            Object message = messageConverter.from(appInstance, timestamp, interval, entry.getValue());
            if (message != null) {
                messages.add(message);
            }
        }
        return messages;
    }

    private String parseDBAddress(String rawUrl) throws URISyntaxException {
        // 去掉jdbc:前缀, 会影响解析
        String originUrl = rawUrl.replaceFirst("jdbc:", "");

        URI uri = new URI(originUrl);

        return uri.getHost() + ":" + uri.getPort();
    }
}
