package com.sbss.bithon.agent.core.plugin;

import com.sbss.bithon.agent.boot.aop.IBithonObject;
import com.sbss.bithon.agent.core.plugin.descriptor.BithonClassDescriptor;
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
     * ALL classes in {@link #getInterceptors()} will be transformed as {@link IBithonObject} automatically.
     * But some classes needs to be transformed too to support passing objects especially those which provide ASYNC ability
     *
     */
    public BithonClassDescriptor getBithonClassDescriptor() {
        return null;
    }

    /**
     * A list, each element of which is an interceptor for a specific method of class
     * NOTE, the target class will be instrumented as {@link IBithonObject}
     */
    public List<InterceptorDescriptor> getInterceptors() {
        return Collections.emptyList();
    }

    public void start() {
    }

    public void stop() {
    }
}
