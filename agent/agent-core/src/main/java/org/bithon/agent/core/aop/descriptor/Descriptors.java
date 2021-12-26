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

package org.bithon.agent.core.aop.descriptor;

import org.bithon.agent.core.aop.precondition.IInterceptorPrecondition;
import org.bithon.agent.core.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Frank Chen
 * @date 26/12/21 11:07 AM
 */
public class Descriptors {
    private final Map<String, Descriptor> descriptors = new HashMap<>();

    public Descriptor get(String type) {
        return descriptors.get(type);
    }

    public void merge(String plugin, BithonClassDescriptor bithonClassDescriptor) {
        if (bithonClassDescriptor == null) {
            return;
        }
        for (String targetClass : bithonClassDescriptor.getTargetClasses()) {
            Descriptor descriptor = this.descriptors.computeIfAbsent(targetClass, v -> new Descriptor(plugin, targetClass));
            if (bithonClassDescriptor.isDebug()) {
                descriptor.isDebuggingOn = bithonClassDescriptor.isDebug();
            }
        }
    }

    public void merge(String plugin, List<IInterceptorPrecondition> preconditions, List<InterceptorDescriptor> interceptors) {
        for (InterceptorDescriptor interceptor : interceptors) {
            String targetClass = interceptor.getTargetClass();

            Descriptor descriptor = this.descriptors.computeIfAbsent(targetClass, v -> new Descriptor(plugin, targetClass));
            descriptor.methodInterceptors.addAll(Stream.of(interceptor.getMethodPointCutDescriptors()).collect(Collectors.toList()));
            if (CollectionUtils.isNotEmpty(preconditions)) {
                descriptor.preconditions.addAll(preconditions);
            }
            descriptor.preconditions.addAll(preconditions);
            if (interceptor.isBootstrapClass()) {
                descriptor.isBootstrapClass = interceptor.isBootstrapClass();
            }
            if (interceptor.isDebug()) {
                descriptor.isDebuggingOn = interceptor.isDebug();
            }
        }
    }

    public Set<String> getTypes() {
        return this.descriptors.keySet();
    }

    public static class Descriptor {
        private final String plugin;
        private final String targetClass;
        private final List<IInterceptorPrecondition> preconditions = new ArrayList<>();
        private final List<MethodPointCutDescriptor> methodInterceptors = new ArrayList<>();
        private boolean isBootstrapClass;
        private boolean isDebuggingOn;

        Descriptor(String plugin, String targetClass) {
            this.plugin = plugin;
            this.targetClass = targetClass;
        }

        /**
         * get the plugin name in which this interceptor is defined.
         */
        public String getPlugin() {
            return plugin;
        }

        public String getTargetClass() {
            return targetClass;
        }

        public List<IInterceptorPrecondition> getPreconditions() {
            return preconditions;
        }

        public List<MethodPointCutDescriptor> getMethodInterceptors() {
            return methodInterceptors;
        }

        public boolean isBootstrapClass() {
            return isBootstrapClass;
        }

        public boolean isDebuggingOn() {
            return isDebuggingOn;
        }
    }
}
