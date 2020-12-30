package com.sbss.bithon.agent.core.transformer.aop;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;

public class Instrumenter {

    private AbstractMethodIntercepted handler;

    private AgentBuilder.Default agentBuilder;

    private Instrumentation inst;

    private String clazz;

    private String methodName;

    private String[] args;

    public Instrumenter(AbstractMethodIntercepted handler, AgentBuilder.Default agentBuilder, Instrumentation inst, String clazz,
                        String methodName, String[] args) {
        this.handler = handler;
        this.agentBuilder = agentBuilder;
        this.inst = inst;
        this.clazz = clazz;
        this.methodName = methodName;
        this.args = args;
    }

    public AbstractMethodIntercepted getHandler() {
        return handler;
    }

    public void setHandler(AbstractMethodIntercepted handler) {
        this.handler = handler;
    }

    public AgentBuilder.Default getAgentBuilder() {
        return agentBuilder;
    }

    public void setAgentBuilder(AgentBuilder.Default agentBuilder) {
        this.agentBuilder = agentBuilder;
    }

    public Instrumentation getInst() {
        return inst;
    }

    public void setInst(Instrumentation inst) {
        this.inst = inst;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }
}
