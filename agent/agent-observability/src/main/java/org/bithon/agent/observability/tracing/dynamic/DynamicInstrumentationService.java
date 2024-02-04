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

package org.bithon.agent.observability.tracing.dynamic;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.configuration.ConfigurationProperties;
import org.bithon.agent.configuration.IConfigurationChangedListener;
import org.bithon.agent.instrumentation.aop.interceptor.installer.DynamicInterceptorInstaller;
import org.bithon.agent.instrumentation.aop.interceptor.installer.Uninstaller;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.agent.starter.IAgentService;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatchers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * a.b.c#method
 * a.b.c
 *
 * @author Frank Chen
 * @date 2/2/24 3:37 pm
 */
public class DynamicInstrumentationService implements IAgentService, IConfigurationChangedListener {

    private Uninstaller uninstaller;

    @Override
    public void start() throws Exception {
        ConfigurationManager.getInstance().addConfigurationChangeListener(this);
    }

    @Override
    public void stop() {
    }

    @Override
    public void onChange(Set<String> keys) {
        if (keys.contains("tracing.instrumentation.methods")) {
            if (uninstaller != null) {
                uninstaller.uninstall();
                uninstaller = null;
            }
            uninstaller = install();
        }
    }

    @ConfigurationProperties(prefix = "tracing.instrumentation.methods")
    static class SignatureList extends ArrayList<String> {
        public SignatureList() {
        }
    }

    static class Methods {
        private boolean all;
        private final Set<String> methods = new HashSet<>();
    }

    public Uninstaller install() {
        SignatureList signatureList = ConfigurationManager.getInstance().getConfig(SignatureList.class);
        if (CollectionUtils.isEmpty(signatureList)) {
            return null;
        }

        Map<String, Methods> descriptors = new HashMap<>();
        for (String signature : signatureList) {
            signature = signature.trim();

            int methodSplitter = signature.indexOf('#');

            String clazzName;
            String method = null;
            if (methodSplitter == -1) {
                clazzName = signature;
            } else {
                clazzName = signature.substring(0, methodSplitter);
                method = signature.substring(methodSplitter + 1);
            }

            Methods m = descriptors.computeIfAbsent(clazzName, k -> new Methods());
            if (method == null) {
                m.all = true;
            } else {
                m.methods.add(method);
            }
        }

        Map<String, DynamicInterceptorInstaller.AopDescriptor> aop = new HashMap<>();
        for (Map.Entry<String, Methods> entry : descriptors.entrySet()) {
            String clazz = entry.getKey();
            Methods methods = entry.getValue();

            aop.put(clazz,
                    new DynamicInterceptorInstaller.AopDescriptor(
                        clazz,
                        methods.all ? ElementMatchers.not(ElementMatchers.isConstructor()) : Matchers.withNames(methods.methods),
                        Interceptor.class.getName()
                    ));
        }
        return DynamicInterceptorInstaller.getInstance()
                                          .install(aop);
    }
}
