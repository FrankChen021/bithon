package com.sbss.bithon.agent.boot.aop;

import com.sbss.bithon.agent.boot.expt.AgentException;
import com.sbss.bithon.agent.boot.loader.AgentDependencyManager;

import java.lang.reflect.Method;

/**
 * ALL methods in this class will be executed in classes which are loaded by bootstrap class loader
 * So, there must be as LESS dependencies as possible for this class
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/19 8:26 下午
 */
public class BootstrapHelper {

    public static IAopLogger createAopLogger(Class<?> logClass) {
        try {
            Class<?> loggerClass = Class.forName("com.sbss.bithon.agent.core.plugin.loader.AopLogger",
                                                 true,
                                                 AgentDependencyManager.getClassLoader());
            Method getLoggerMethod = loggerClass.getDeclaredMethod("getLogger", Class.class);
            return (IAopLogger) getLoggerMethod.invoke(null, logClass);
        } catch (Exception e) {
            throw new AgentException(e);
        }
    }
}
