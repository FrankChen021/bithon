package com.sbss.bithon.agent.plugin.jedis;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.BeforeJoinPoint;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * @author frankchen
 * @Date Dec 26, 2019 12:11:02 PM
 */
public class JedisOutputStreamHandler extends AbstractMethodIntercepted {
    private static final Logger log = LoggerFactory.getLogger(JedisOutputStreamHandler.class);

    private Field countField;
    private RedisCounter counter;

    @Override
    public boolean init() {
        counter = RedisCounter.getInstance();
        return true;
    }

    /**
     * AOP flushBuffer方法，需要在执行该方法前获取count属性
     */
    @Override
    protected void before(BeforeJoinPoint joinPoint) {
        Object outputStream = joinPoint.getTarget(); // RedisOutputStream
        if (!ensureField(outputStream.getClass()))
            return;

        try {
            int bytesOut = countField.getInt(outputStream);
            counter.countOutputBytes(joinPoint.getTarget(), bytesOut);
            if (log.isDebugEnabled()) {
                log.debug("before RedisOutputStream.flushBuffer, count {}", bytesOut);
            }
        } catch (IllegalArgumentException
            | IllegalAccessException e) {
            log.error("before RedisOutputStream.flushBuffer exception: {}", e);
        }
    }

    private boolean ensureField(Class<?> clazz) {
        try {
            if (countField == null) {
                // 先赋值给临时变量，否则在并发时可能会导致在未调用setAccessible时就获取值
                Field f = clazz.getDeclaredField("count");
                f.setAccessible(true);
                countField = f;
            }
            return true;
        } catch (NoSuchFieldException e) {
            log.warn("cannot access field [count] of redis output stream", e);
            return false;
        }
    }
}
