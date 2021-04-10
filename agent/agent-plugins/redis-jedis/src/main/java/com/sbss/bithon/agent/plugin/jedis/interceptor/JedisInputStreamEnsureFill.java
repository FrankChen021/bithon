package com.sbss.bithon.agent.plugin.jedis.interceptor;

import com.sbss.bithon.agent.boot.aop.AbstractInterceptor;
import com.sbss.bithon.agent.boot.aop.AopContext;
import com.sbss.bithon.agent.boot.aop.IBithonObject;
import com.sbss.bithon.agent.core.context.InterceptorContext;
import com.sbss.bithon.agent.core.metric.collector.MetricCollectorManager;
import com.sbss.bithon.agent.core.metric.domain.redis.RedisMetricCollector;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * @author frankchen
 * @date Dec 26, 2020 12:11:14 PM
 */
public class JedisInputStreamEnsureFill extends AbstractInterceptor {
    private static final Logger log = LoggerFactory.getLogger(JedisInputStreamEnsureFill.class);

    private Field countField = null;
    private Field limitField = null;
    private RedisMetricCollector metricCollector;

    @Override
    public boolean initialize() {
        metricCollector = MetricCollectorManager.getInstance().getOrRegister("jedis", RedisMetricCollector.class);
        return true;
    }

    /**
     * The endpoint object returned by 'getInjectedObject' method is set in {@link JedisConnectionConnect}
     */
    @Override
    public void onMethodLeave(AopContext aopContext) {
        if (aopContext.hasException()) {
            return;
        }

        // since RedisInputStream is in different package,
        // DO NOT use its qualified type to define inputStream object below
        Object inputStream = aopContext.getTarget();
        if (!ensureField(inputStream.getClass())) {
            return;
        }

        try {
            // countField: offset of buffer in InputStream
            // limitField: length of buffer in InputStream
            // enfill maybe called many times by read function
            // when count>=limit, RedisInputStream reads data from underlying stream
            int inputBytes = countField.getInt(inputStream);
            if (inputBytes > 0) {
                return;
            }

            String command = InterceptorContext.getAs("redis-command");
            int bytesIn = limitField.getInt(inputStream);
            metricCollector.addInputBytes((String) ((IBithonObject) inputStream).getInjectedObject(),
                                          command,
                                          bytesIn);
        } catch (IllegalArgumentException
            | IllegalAccessException e) {
            log.error("cannot access field [limit/count] of RedisInputStream", e);
        }
    }

    private boolean ensureField(Class<?> clazz) {
        try {
            if (countField == null) {
                // assign to temporary field first, or there may be concurrent problems
                Field field = clazz.getDeclaredField("count");
                field.setAccessible(true);
                this.countField = field;
            }
            if (limitField == null) {
                // assign to temporary field first, or there may be concurrent problems
                Field field = clazz.getDeclaredField("limit");
                field.setAccessible(true);
                this.limitField = field;
            }
            return true;
        } catch (NoSuchFieldException
            | SecurityException e) {
            log.error("cannot access field [limit/count] of RedisInputStream", e);
            return false;
        }
    }
}
