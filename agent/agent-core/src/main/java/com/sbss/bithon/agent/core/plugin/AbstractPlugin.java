package com.sbss.bithon.agent.core.plugin;

import com.sbss.bithon.agent.core.plugin.descriptor.InterceptorDescriptor;
import com.sbss.bithon.agent.core.plugin.precondition.IPluginInstallationChecker;

import java.util.Collections;
import java.util.List;

/**
 * @author frankchen
 * @date 2020-12-31 22:29:55
 */
public abstract class AbstractPlugin {

    public List<IPluginInstallationChecker> getCheckers() {
        return Collections.emptyList();
    }

    /**
     * A list, each element of which is a qualified name of a class which will be instrumented as {@link com.sbss.bithon.agent.core.plugin.aop.bootstrap.IBithonObject}
     */
    public String[] getClassInstrumentions() {
        return new String[0];
    }

    /**
     * A list, each element of which is an interceptor for a specific method of class
     * NOTE, the target class will be instrumented as {@link com.sbss.bithon.agent.core.plugin.aop.bootstrap.IBithonObject}
     *
     * @return
     */
    public List<InterceptorDescriptor> getInterceptors() {
        return Collections.emptyList();
    }

    public void start() {
    }

    public void stop() {
    }
}
