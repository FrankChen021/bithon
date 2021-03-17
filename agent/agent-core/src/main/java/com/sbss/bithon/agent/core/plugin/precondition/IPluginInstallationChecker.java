package com.sbss.bithon.agent.core.plugin.precondition;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import shaded.net.bytebuddy.description.type.TypeDescription;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/17 8:13 下午
 */
public interface IPluginInstallationChecker {

    /**
     * Helper method
     */
    static IPluginInstallationChecker hasClass(String className) {
        return new HasClassChecker(className, false);
    }

    /**
     * Helper method
     */
    static IPluginInstallationChecker hasClass(String className, boolean debugging) {
        return new HasClassChecker(className, debugging);
    }

    static IPluginInstallationChecker or(IPluginInstallationChecker... checkers) {
        return new OrChecker(checkers);
    }

    /**
     * returns true if interceptors in this plugin can be installed
     *
     * @param plugin          plugin of which interceptors are being installed
     * @param typeDescription
     */
    boolean canInstall(AbstractPlugin plugin, ClassLoader classLoader, TypeDescription typeDescription);
}
