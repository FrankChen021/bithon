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

package org.bithon.agent.instrumentation.aop.interceptor.installer;

import org.bithon.agent.instrumentation.logging.ILogger;
import org.bithon.agent.instrumentation.logging.LoggerFactory;
import org.bithon.shaded.net.bytebuddy.agent.builder.AgentBuilder;
import org.bithon.shaded.net.bytebuddy.agent.builder.ResettableClassFileTransformer;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Frank Chen
 * @date 2/2/24 6:22 pm
 */
public class Uninstaller extends AgentBuilder.InstallationListener.Adapter {
    private static final ILogger LOG = LoggerFactory.getLogger(Uninstaller.class);

    private ResettableClassFileTransformer transformer;
    private final Instrumentation instance;
    private final Map<String, Set<String>> interceptors = new HashMap<>();

    public Uninstaller(Instrumentation instance,
                       Map<String, DynamicInterceptorInstaller.AopDescriptor> descriptors) {
        this.instance = instance;
        descriptors.forEach((clazz, aop) -> interceptors.computeIfAbsent(aop.getInterceptorName(), k -> new HashSet<>())
                                                        .add(clazz));
    }

    public void setTransformer(ResettableClassFileTransformer transformer) {
        this.transformer = transformer;
    }

    public void uninstall() {
        transformer.reset(instance, AgentBuilder.RedefinitionStrategy.REDEFINITION);
    }

    @Override
    public void onReset(Instrumentation instrumentation, ResettableClassFileTransformer classFileTransformer) {
        LOG.info("Interceptors uninstalled.");

        InstallerRecorder.INSTANCE.deleteInterceptorIf((m) -> {
            Set<String> clazzList = this.interceptors.get(m.getInterceptorName());
            if (clazzList != null) {
                return clazzList.contains(m.getType());
            } else {
                return false;
            }
        });
    }
}
