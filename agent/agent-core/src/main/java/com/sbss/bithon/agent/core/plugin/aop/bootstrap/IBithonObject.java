package com.sbss.bithon.agent.core.plugin.aop.bootstrap;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/21 9:37 下午
 */
public interface IBithonObject {

    String INJECTED_FIELD_NAME = "_$BITHON_INJECTED_FIELD_";

    Object getInjectedObject();

    void setInjectedObject(Object value);
}