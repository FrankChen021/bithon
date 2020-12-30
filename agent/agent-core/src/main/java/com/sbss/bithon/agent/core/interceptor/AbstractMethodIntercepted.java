package com.sbss.bithon.agent.core.interceptor;

/**
 * 用于切面, 定义切点的增强操作
 *
 * @author lizheng
 * @author mazy
 */
public abstract class AbstractMethodIntercepted {

    /**
     * 用于普通切面的初始化, 返回值决定是否要加载插件
     *
     * @return true-初始化成功， false-初始化失败
     * @throws Exception 切面初始化增强所抛出的异常
     */
    public abstract boolean init() throws Exception;

    /**
     * 用于普通切面的切点执行前的方法
     *
     * @param joinPoint 切点
     */
    protected void before(BeforeJoinPoint joinPoint) {

    }

    /**
     * 在切点执行之前为其增加一道上下文
     *
     * @param joinPoint 切点
     * @return 切面为切点附加的上下文
     */
    protected Object createContext(BeforeJoinPoint joinPoint) {
        return null;
    }

    /**
     * 用于普通切面的切点执行后的方法
     *
     * @param joinPoint 切点
     */
    protected void after(AfterJoinPoint joinPoint) {

    }

    /**
     * 用于构造器切面的方法, 将会在构造器执行后执行此方法
     *
     * @param constructedObject 构造器所生成的实例
     * @param args              构造器方法的参数
     * @throws Exception 切面执行所产生的异常
     */
    protected void onConstruct(Object constructedObject,
                               Object[] args) throws Exception {
    }
}
