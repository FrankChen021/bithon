package com.sbss.bithon.agent.core.plugin.precondition;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/17 8:14 下午
 */
class HasClassChecker implements IPluginInstallationChecker {

    private final String className;
    private final boolean debugging;

    public HasClassChecker(String className, boolean debugging) {
        this.className = className;
        this.debugging = debugging;
    }

    @Override
    public boolean canInstall(AbstractPlugin plugin,
                              ClassLoader classLoader,
                              TypeDescription typeDescription) {
        boolean resolved = TypeResolver.getInstance().isResolved(classLoader, this.className);
        if (!resolved && this.debugging) {
            LoggerFactory.getLogger(HasClassChecker.class)
                    .info("Required class [{}] not found to install interceptors to [{}] in plugin [{}]",
                            this.className,
                            typeDescription.getName(),
                            plugin.getClass().getSimpleName());
        }
        return resolved;
    }
}
