package com.sbss.bithon.agent.core.plugin.loader;

import com.sbss.bithon.agent.core.plugin.aop.bootstrap.IAopLogger;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * created via reflection from bootstrap aop instances which are loaded by bootstrap class loader
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/19 10:49 下午
 */
public class AopLogger implements IAopLogger {

    private final Logger log;

    private AopLogger(Class<?> logClass) {
        this.log = LoggerFactory.getLogger(logClass);
    }

    public static IAopLogger getLogger(Class<?> clazz) {
        return new AopLogger(clazz);
    }

    @Override
    public void warn(String message, Throwable e) {
        log.warn(message, e);
    }

    @Override
    public void error(String message, Throwable e) {
        log.error(message, e);
    }
}
