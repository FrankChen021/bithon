package com.sbss.bithon.agent.core.plugin.precondition;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import shaded.net.bytebuddy.description.type.TypeDescription;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/15
 */
public class OrChecker implements IPluginInstallationChecker {
    private final IPluginInstallationChecker[] checkers;

    public OrChecker(IPluginInstallationChecker[] checkers) {
        this.checkers = checkers;
    }

    @Override
    public boolean canInstall(AbstractPlugin plugin, ClassLoader classLoader, TypeDescription typeDescription) {
        for (IPluginInstallationChecker checker : checkers) {
            if (checker.canInstall(plugin, classLoader, typeDescription)) {
                return true;
            }
        }
        return false;
    }
}
