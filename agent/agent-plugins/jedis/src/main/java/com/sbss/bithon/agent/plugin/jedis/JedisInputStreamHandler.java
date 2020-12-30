package com.sbss.bithon.agent.plugin.jedis;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * @author frankchen
 * @Date Dec 26, 2019 12:11:14 PM
 */
public class JedisInputStreamHandler extends AbstractMethodIntercepted {
    private static final Logger log = LoggerFactory.getLogger(JedisInputStreamHandler.class);

    private Field countField = null;
    private Field limitField = null;
    private RedisCounter counter;

    @Override
    public boolean init() {
        counter = RedisCounter.getInstance();
        return true;
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        if (joinPoint.getException() != null)
            return;

        Object inputStream = joinPoint.getTarget(); // RedisInputStream
        if (!ensureField(inputStream.getClass()))
            return;

        try {
            // count字段在RedisInputStream表示当前InputStream内部buf读取字节数或偏移
            // limit表示当前内部buf的有效长度
            // enfill函数会被read函数多次调用，仅当count>=limit时，InputStrema才会从底层读取数据
            // 之后count会被置为零
            int count = countField.getInt(inputStream);
            if (count > 0)
                return;

            int bytesIn = limitField.getInt(inputStream);
            counter.countInputBytes(inputStream, bytesIn);

            if (log.isDebugEnabled()) {
                log.debug("after RedisInputStream.enfill, count {}", bytesIn);
            }
        } catch (IllegalArgumentException
            | IllegalAccessException e) {
            log.error("cannot access field [limit/count] of RedisInputStream", e);
            return;
        }
    }

    private boolean ensureField(Class<?> clazz) {
        try {
            if (countField == null) {
                // 先赋值给临时变量，否则在并发时可能会导致在未调用setAccessible时就获取值
                Field f = clazz.getDeclaredField("count");
                f.setAccessible(true);
                this.countField = f;
            }
            if (limitField == null) {
                // 先赋值给临时变量，否则在并发时可能会导致在未调用setAccessible时就获取值
                Field f = clazz.getDeclaredField("limit");
                f.setAccessible(true);
                this.limitField = f;
            }
            return true;
        } catch (NoSuchFieldException
            | SecurityException e) {
            log.error("cannot access field [limit/count] of RedisInputStream", e);
            return false;
        }
    }
}