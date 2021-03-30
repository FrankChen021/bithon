package com.sbss.bithon.agent.core.plugin.aop.bootstrap;

/**
 * See {@link net.bytebuddy.implementation.bind.annotation.Morph} for how this interface works
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/30 22:49
 */
public interface ISuperMethod {
    Object invoke(Object[] args);
}
