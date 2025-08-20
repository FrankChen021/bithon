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

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.configuration.annotation.ConfigurationProperties;
import org.bithon.agent.configuration.annotation.PropertyDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.matcher.Matchers;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frank.chen021@outlook.com
 * @date 2022-05-12
 */
public class BithonBrpcPlugin implements IPlugin {

    @ConfigurationProperties(path = "agent.plugin.bithon.brpc", dynamic = false)
    public static class ServiceProviderConfig {
        @PropertyDescriptor(
            description = "The BRPC services that will be instrumented. The key is the service class name while the value is the BRPC interfaces that the service implements, separated by comma.",
            required = false
        )
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
        ServiceProviderConfig config = ConfigurationManager.getInstance().getConfig(ServiceProviderConfig.class);
        if (config == null) {
            return Collections.emptyList();
        }

        return config.getProviders()
                     .keySet()
                     .stream()
                     .map((provider) -> {
            String[] interfaces = config.getProviders().get(provider).split(",");

            return forClass(provider).onMethod(Matchers.implement(interfaces))
                                     .interceptedBy("org.bithon.agent.plugin.bithon.brpc.interceptor.BrpcMethodInterceptor")
                                     .build();

        }).collect(Collectors.toList());
    }
}
