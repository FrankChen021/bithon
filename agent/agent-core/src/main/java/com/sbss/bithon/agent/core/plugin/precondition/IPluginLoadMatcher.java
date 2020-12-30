package com.sbss.bithon.agent.core.plugin.precondition;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/17 8:13 下午
 */
public interface IPluginLoadMatcher {
    boolean canLoad(PluginLoadingContext context);

    static IPluginLoadMatcher hasClass(String className) {
        return new HasClassMatcher(className);
    }
}
