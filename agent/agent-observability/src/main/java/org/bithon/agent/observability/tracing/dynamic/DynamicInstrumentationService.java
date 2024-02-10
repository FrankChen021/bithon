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
import org.bithon.agent.instrumentation.logging.ILogger;
import org.bithon.agent.instrumentation.logging.LoggerFactory;
import org.bithon.agent.starter.IAgentService;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.StringUtils;
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

    private static final ILogger LOG = LoggerFactory.getLogger(DynamicInstrumentationService.class);

    private Uninstaller uninstaller;

    @Override
    public void start() throws Exception {
        ConfigurationManager.getInstance().addConfigurationChangedListener("tracing.instrumentation.methods", this);
    }

    @Override
    public void stop() {
    }

    @Override
    public void onChange() {
        if (uninstaller != null) {
            LOG.info("Instrumentation methods changed, uninstalling interceptors first...");
            uninstaller.uninstall();
            uninstaller = null;
        }

        uninstaller = install();
    }

    @ConfigurationProperties(prefix = "tracing.instrumentation.methods")
    static class SignatureList extends ArrayList<String> {
        public SignatureList() {
        }
    }

    static class InstrumentedMethod {
        private final Set<String> methods = new HashSet<>();
    }

    public Uninstaller install() {
        SignatureList signatureList = ConfigurationManager.getInstance().getConfig(SignatureList.class);
        if (CollectionUtils.isEmpty(signatureList)) {
            return null;
        }

        LOG.info("Instrumentation methods changed, install new interceptors...");

        Map<String, InstrumentedMethod> instrumentedMethods = new HashMap<>();
        for (String signature : signatureList) {
            signature = signature.trim();

            int methodSplitter = signature.indexOf('#');

            String clazzName;
            String method = null;
            if (methodSplitter == -1) {
                clazzName = signature;
            } else {
                clazzName = signature.substring(0, methodSplitter);
                method = signature.substring(methodSplitter + 1).trim();
            }

            InstrumentedMethod m = instrumentedMethods.computeIfAbsent(clazzName, k -> new InstrumentedMethod());
            if (StringUtils.hasText(method)) {
                m.methods.add(method);
            }
        }

        Map<String, DynamicInterceptorInstaller.AopDescriptor> aop = new HashMap<>();
        for (Map.Entry<String, InstrumentedMethod> entry : instrumentedMethods.entrySet()) {
            String clazz = entry.getKey();
            InstrumentedMethod instrumentedMethod = entry.getValue();

            aop.put(clazz,
                    new DynamicInterceptorInstaller.AopDescriptor(
                        clazz,
                        instrumentedMethod.methods.isEmpty() ? ElementMatchers.not(ElementMatchers.isConstructor()) : Matchers.withNames(instrumentedMethod.methods),
                        Interceptor.class.getName()
                    ));
        }
        return DynamicInterceptorInstaller.getInstance()
                                          .install(aop);
    }
}
