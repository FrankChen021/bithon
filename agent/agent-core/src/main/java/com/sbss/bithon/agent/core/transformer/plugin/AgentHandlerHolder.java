package com.sbss.bithon.agent.core.transformer.plugin;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.IMethodInterceptor;
import com.sbss.bithon.agent.core.loader.AgentClassloader;
import com.sbss.bithon.agent.core.loader.AgentClassloaderManager;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class AgentHandlerHolder {

    private static final Logger log = LoggerFactory.getLogger(AgentHandlerHolder.class);

    /**
     * 将handlers修改为的Map, 保证handler class不会由于pointcut的触发,
     * 导致其被同一个classloader重复多次实例创建<br>
     * </br>
     * key : classloader_hash + handlerClassName value: eventCallback handler
     */
    private static Map<String, AbstractMethodIntercepted> handlers = new HashMap<>();

    private static ReentrantLock handlerLock = new ReentrantLock();

    public static AbstractMethodIntercepted get(Class<?> clazz) {
        Set<AbstractMethodIntercepted> values = new HashSet<>(handlers.values());
        return values.stream().filter(x -> x.getClass() == clazz).findFirst().orElse(null);
    }

    /**
     * 根据classloader延迟加载handler, 并将其初始化, 将原来的add方法私有化至此
     *
     * @param handlerClass 需要加载的class
     * @param classLoader  切点的classloader
     * @param properties   需要添加的properties
     * @return 夹在并完成初始化的handler
     */
    public static AbstractMethodIntercepted lazyLoadHandler(String handlerClass,
                                                            ClassLoader classLoader) throws Exception {
        // 先搜索handler是否已经存在, 以避免重复加载
        String handlerId = generateHandlerId(handlerClass, classLoader);

        handlerLock.lock();
        try {
            AbstractMethodIntercepted handler = handlers.get(handlerId);
            if (handler == null) {
                ClassLoader handlerLoader = AgentClassloaderManager.getMappingClassloader(classLoader);

                handler = (AbstractMethodIntercepted) Class.forName(handlerClass, true, handlerLoader).newInstance();
                boolean handlerInitialed = handler.init();
                if (!handlerInitialed) {
                    handler = null;
                } else {
                    log.info("{} initialized", handler.getClass().getSimpleName());
                    handlers.put(handlerId, handler);
                }
            }

            return handler;
        } finally {
            handlerLock.unlock();
        }
    }

    /**
     * 使用agent默认classloader直接加载handler, 并将其初始化, 将原来的add方法私有化至此
     *
     * @param handlerClass 需要加载的class
     */
    public static void directLoadHandler(String handlerClass) throws Exception {
        ClassLoader classLoader = AgentClassloader.getDefaultInstance();
        String handlerId = generateHandlerId(handlerClass, classLoader);

        handlerLock.lock();
        try {
            AbstractMethodIntercepted handler = handlers.get(handlerId);
            if (handler == null) {
                handler = (AbstractMethodIntercepted) Class.forName(handlerClass, true, classLoader).newInstance();
                handler.init();
                log.info("{} initialized", handler.getClass().getSimpleName());
                handlers.put(handlerId, handler);
            }
        } finally {
            handlerLock.unlock();
        }
    }

    /**
     * 创建handler id
     *
     * @param handlerClass handler class
     * @param loader       classloader
     * @return unique handler id
     */
    private static String generateHandlerId(String handlerClass,
                                            ClassLoader loader) {
        if (null == loader) {
            // Bootstrap Classloader
            return "bootstrap" + handlerClass;
        }
        return loader.hashCode() + handlerClass;
    }

    public static IMethodInterceptor lazyLoadIntercepter(String interceptorClassName,
                                                         ClassLoader classLoader) throws Exception {
        ClassLoader agentLoader = AgentClassloaderManager.getMappingClassloader(classLoader);
        return (IMethodInterceptor) agentLoader.loadClass(interceptorClassName).newInstance();
    }
}
