package com.sbss.bithon.agent.boot.aop;

/**
 * Since Aop, which is injected into bootstrap class loader, depends on log,
 * and shaded.slf4j is not loaded by bootstrap class loader, we provide this class for Aop to log
 * <p>
 * NOTE: this class is injected into Bootstrap class loader
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/19 10:45 下午
 */
public interface IAopLogger {

    void warn(String message, Throwable e);

    void error(String message, Throwable e);
}
