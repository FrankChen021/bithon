/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.agent.core.interceptor.descriptor;

import org.bithon.agent.core.interceptor.precondition.IInterceptorPrecondition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 26/12/21 11:07 AM
 */
public class Descriptors {
    /**
     * key - target class name
     */
    private final Map<String, Descriptor> descriptors = new HashMap<>();

    public Descriptor get(String type) {
        return descriptors.get(type);
    }

    /**
     * merge class instrumentation
     */
    public void merge(BithonClassDescriptor bithonClassDescriptor) {
        if (bithonClassDescriptor == null) {
            return;
        }
        for (String targetClass : bithonClassDescriptor.getTargetClasses()) {
            Descriptor descriptor = this.descriptors.computeIfAbsent(targetClass, v -> new Descriptor(targetClass));
            if (bithonClassDescriptor.isDebug()) {
                descriptor.isDebuggingOn = bithonClassDescriptor.isDebug();
            }
        }
    }

    /**
     * merge method instrumentation
     */
    public void merge(String plugin, IInterceptorPrecondition preconditions, List<InterceptorDescriptor> interceptors) {
        for (InterceptorDescriptor interceptor : interceptors) {
            String targetClass = interceptor.getTargetClass();

            Descriptor descriptor = this.descriptors.computeIfAbsent(targetClass, v -> new Descriptor(targetClass));
            if (interceptor.isDebug()) {
                descriptor.isDebuggingOn = interceptor.isDebug();
            }

            MethodPointCuts mp = new MethodPointCuts(plugin, preconditions, interceptor.getMethodPointCutDescriptors());
            descriptor.getMethodPointCuts().add(mp);
        }
    }

    public Set<String> getTypes() {
        return this.descriptors.keySet();
    }

    public Collection<Descriptor> getAllDescriptor() {
        return this.descriptors.values();
    }

    public static class Descriptor {
        private final String targetClass;
        private final List<MethodPointCuts> methodPointCuts = new ArrayList<>();
        private boolean isDebuggingOn;

        Descriptor(String targetClass) {
            this.targetClass = targetClass;
        }

        public List<MethodPointCuts> getMethodPointCuts() {
            return methodPointCuts;
        }

        public String getTargetClass() {
            return targetClass;
        }

        public boolean isDebuggingOn() {
            return isDebuggingOn;
        }
    }

    public static class MethodPointCuts {
        public String getPlugin() {
            return plugin;
        }

        public IInterceptorPrecondition getPrecondition() {
            return precondition;
        }

        public MethodPointCutDescriptor[] getMethodInterceptors() {
            return methodInterceptors;
        }

        private final String plugin;
        private final IInterceptorPrecondition precondition;
        private final MethodPointCutDescriptor[] methodInterceptors;

        public MethodPointCuts(String plugin,
                               IInterceptorPrecondition preconditions,
                               MethodPointCutDescriptor[] methodInterceptors) {
            this.plugin = plugin;
            this.precondition = preconditions;
            this.methodInterceptors = methodInterceptors;
        }
    }
}
