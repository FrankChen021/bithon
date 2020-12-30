package com.sbss.bithon.agent.core.plugin.precondition;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/17 8:14 下午
 */
class HasClassMatcher implements IPluginLoadMatcher {

    private final String className;

    public HasClassMatcher(String className) {
        this.className = className;
    }

    @Override
    public boolean canLoad(PluginLoadingContext context) {
        return context.getTypePool().describe(className).isResolved();
    }
}
