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

package org.bithon.agent.plugin.bithon.brpc;

import org.bithon.agent.core.aop.descriptor.InterceptorDescriptor;
import org.bithon.agent.core.aop.descriptor.MethodPointCutDescriptorBuilder;
import org.bithon.agent.core.config.AgentConfiguration;
import org.bithon.agent.core.config.ConfigurationProperties;
import org.bithon.agent.core.plugin.IPlugin;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatchers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.bithon.agent.core.aop.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frank.chen021@outlook.com
 * @date 2022-05-12
 */
public class BithonBrpcPlugin implements IPlugin {

    @ConfigurationProperties(prefix = "agent.plugin.bithon.brpc")
    public static class ServiceProviderConfig {
        private Map<String, String> providers = Collections.emptyMap();

        public Map<String, String> getProviders() {
            return providers;
        }

        public void setProviders(Map<String, String> providers) {
            this.providers = providers;
        }
    }

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        ServiceProviderConfig config = AgentConfiguration.getInstance().getConfig(ServiceProviderConfig.class);
        if (config == null) {
            return Collections.emptyList();
        }

        return config.getProviders().keySet().stream().map((provider) -> {
            String[] interfaces = config.getProviders().get(provider).split(",");

            return forClass(provider).methods(
                MethodPointCutDescriptorBuilder.build()
                                               .onMethod(ElementMatchers.isPublic()
                                                                        .and(ElementMatchers.not(ElementMatchers.isDefaultMethod()))
                                                                        .and(ElementMatchers.isOverriddenFrom(ElementMatchers.namedOneOf(interfaces))))
                                               .to("org.bithon.agent.plugin.bithon.brpc.interceptor.BrpcMethodInterceptor")
            );
        }).collect(Collectors.toList());
    }
}
