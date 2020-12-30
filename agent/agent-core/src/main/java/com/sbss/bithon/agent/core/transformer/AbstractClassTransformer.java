package com.sbss.bithon.agent.core.transformer;

import com.sbss.bithon.agent.core.interceptor.*;
import com.sbss.bithon.agent.core.transformer.matcher.AgentMethodMatcher;
import com.sbss.bithon.agent.core.transformer.plugin.AgentHandlerHolder;
import com.sbss.bithon.agent.core.transformer.plugin.IAgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.IMethodPointCut;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;
import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.dynamic.DynamicType;
import shaded.net.bytebuddy.implementation.MethodDelegation;
import shaded.net.bytebuddy.implementation.SuperMethodCall;
import shaded.net.bytebuddy.utility.JavaModule;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.instrument.Instrumentation;

import static com.sbss.bithon.agent.core.transformer.matcher.AgentMethodMatcher.AgentMethodType.*;

/**
 * plugin配置定义
 *
 * @author lizheng
 * @author mazy
 */
public abstract class AbstractClassTransformer {

    private static final Logger log = LoggerFactory.getLogger(AbstractClassTransformer.class);

    /**
     * 获取plugin配置(切面配置)
     *
     * @return plugin配置
     */
    public abstract IAgentHandler[] getHandlers();

    /**
     * 根据插件定义的handlers配置增强操作
     *
     * @param bootstrapClassloaderTmpDir boot class tmp dir,
     *                                   添加到这里的class文件会被bootstrap classloader加载
     * @param inst                       app inst
     */
    public void enhance(AgentBuilder agentBuilder,
                        File bootstrapClassloaderTmpDir,
                        Instrumentation inst) {
        for (IAgentHandler agentHandler : getHandlers()) {
            // 配置插件的各类切面
            setupHandler(agentBuilder, bootstrapClassloaderTmpDir, agentHandler, inst);
        }
    }

    /**
     * handler setup<br>
     * </br>
     * 如果handler定义了pointcut, 那么根据pointcut匹配延迟加载handler<br>
     * </br>
     * 如果没有定义pointcut, 那么直接加载
     *
     * @param agentBuilder               bytebuddy agent builder
     * @param bootstrapClassloaderTmpDir 附加的bootstrap tmp 目录
     * @param agentHandler               定义的agent
     * @param inst                       需要增强的application instance
     */
    private void setupHandler(AgentBuilder agentBuilder,
                              File bootstrapClassloaderTmpDir,
                              IAgentHandler agentHandler,
                              Instrumentation inst) {
        if (null == agentHandler.getPointcuts() || 0 == agentHandler.getPointcuts().length) {
            try {
                AgentHandlerHolder.directLoadHandler(agentHandler.getHandlerClass());
            } catch (Exception e) {
                log.error(String.format("direct load agent handler %s initialize failed due to %s",
                                        agentHandler.getHandlerClass(),
                                        e.getMessage()));
            }
        } else {
            for (IMethodPointCut agentPointcut : agentHandler.getPointcuts()) {
                agentBuilder
//                        .ignore(ElementMatchers.none())
                    .enableBootstrapInjection(inst, bootstrapClassloaderTmpDir)
                    .type(agentPointcut.getClassMatcher().getMatcher())
                    .transform((DynamicType.Builder<?> builder,
                                TypeDescription typeDescription,
                                ClassLoader classLoader,
                                JavaModule javaModule) -> defineAgentTypeBuilder(builder,
                                                                                 agentHandler.getHandlerClass(),
                                                                                 classLoader,
                                                                                 agentPointcut.getMethodMatcher()))
//                        .with(AgentBuilder.Listener.StreamWriting.toSystemOut())
                    .installOn(inst);
            }
        }
    }

    /**
     * 用于定义当前匹配到的class, 需要进行哪些增强
     *
     * @param builder            bytebuddy type builder
     * @param enhanceHandlerName class enhance handler name
     * @param classLoader        current type classloader
     * @param agentMethodMatcher method matcher
     * @param properties         agent handler properties
     * @return a modified type builder with enhance definition
     */
    private DynamicType.Builder<?> defineAgentTypeBuilder(DynamicType.Builder<?> builder,
                                                          String enhanceHandlerName,
                                                          ClassLoader classLoader,
                                                          AgentMethodMatcher agentMethodMatcher) {
        // 当类型匹配成功时, 先尝试初始化这个plugin对应配置文件中定义的切面handler, 这样可以保证所有切面都能够成功加载
        AbstractMethodIntercepted enhanceHandler;
        try {
            // 在此处, 尝试用切点对应的classloader去加载增强handler, 这样可以保证不报强制类型转换错误, 提高开发效率, 更主要是减少反射
            enhanceHandler = AgentHandlerHolder.lazyLoadHandler(enhanceHandlerName, classLoader);

            if (null == enhanceHandler) {
                log.info("agent handler {} initial failed, interceptor ignored", enhanceHandlerName);
                return null;
            }
        } catch (Exception e) {
            log.error("lazy load agent handler {} initialize failed due to {}", enhanceHandlerName, e.getMessage());
            return null;
        }

        // 为定义的方法, 加载一个切面
        IMethodInterceptor interceptor;
        String interceptorName = "";

        try {
            // 方法可能定义为, 构造方法 & 一般静态方法 & 一般非静态方法, 区别加载
            switch (agentMethodMatcher.getMethodMatcherType()) {
                case NORMAL:
                    interceptorName = NORMAL.getInterceptorName();
                    interceptor = AgentHandlerHolder.lazyLoadIntercepter(interceptorName, classLoader);
                    ((AroundMethodInterceptor) interceptor).setEventCallback(enhanceHandler);
                    builder = builder.method(agentMethodMatcher.getMatcher()).intercept(MethodDelegation.to(interceptor));
                    break;
                case CONSTRUCTOR:
                    interceptorName = CONSTRUCTOR.getInterceptorName();
                    interceptor = AgentHandlerHolder.lazyLoadIntercepter(interceptorName, classLoader);
                    ((ConstructorInterceptor) interceptor).setEventCallback(enhanceHandler);
                    builder = builder.constructor(agentMethodMatcher.getMatcher())
                        .intercept(SuperMethodCall.INSTANCE.andThen(MethodDelegation.to(interceptor)));
                    break;
                case STATIC:
                    interceptorName = STATIC.getInterceptorName();
                    interceptor = AgentHandlerHolder.lazyLoadIntercepter(interceptorName, classLoader);
                    ((StaticMethodInterceptor) interceptor).setEventCallback(enhanceHandler);
                    builder = builder.method(agentMethodMatcher.getMatcher()).intercept(MethodDelegation.to(interceptor));
                    break;
                default:
                    log.warn("method matcher type not defined, ignored the pointcut!");
                    break;
            }
        } catch (Exception e) {
            log.error("lazy load agent interceptor {} for handler {} failed due to {}",
                      interceptorName,
                      enhanceHandlerName,
                      e.getMessage());
            return null;
        }

        log.debug(String.format("Instrument pointcut for handler class %s, with method %s)",
                                enhanceHandlerName,
                                agentMethodMatcher.toString()));

        return builder;
    }
}
