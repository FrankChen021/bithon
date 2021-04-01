package com.sbss.bithon.agent.core.plugin.aop.bootstrap;

import java.lang.reflect.Method;

/**
 * ALL methods in this class will be executed in classes which are loaded by bootstrap class loader
 * So, there must be as LESS dependencies as possible for this class
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/19 8:26 下午
 */
public class BootstrapHelper {

    static ClassLoader defaultAgentClassLoader = null;

    /**
     * this method is called within bootstrap AOP which is loaded by bootstrap class loader
     * interceptors are defined in plugins which are loaded by Agent Class loader
     * we must use reflection to get the Default Agent Class Loader which has been instantiated when agent starts
     */
    public static ClassLoader getAgentClassLoader() {
        if (defaultAgentClassLoader != null) {
            return defaultAgentClassLoader;
        }

        // no need to sync, so no lock is required to eliminate potential dead lock
        try {
            Class<?> agentClassLoaderClass = Class.forName("com.sbss.bithon.agent.bootstrap.AgentClassLoader");
            Method getInstanceMethod = agentClassLoaderClass.getDeclaredMethod("getDefaultInstance");
            getInstanceMethod.setAccessible(true);
            defaultAgentClassLoader = (ClassLoader) getInstanceMethod.invoke(null);
            return defaultAgentClassLoader;
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return null;
        }
    }

    public static IAopLogger createAopLogger(ClassLoader defaultAgentClassLoader,
                                             Class<?> logClass) {
        try {
            Class<?> loggerClass = Class.forName("com.sbss.bithon.agent.core.plugin.aop.AopLogger",
                                                 true,
                                                 defaultAgentClassLoader);
            Method getLoggerMethod = loggerClass.getDeclaredMethod("getLogger", Class.class);
            return (IAopLogger) getLoggerMethod.invoke(null, logClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
