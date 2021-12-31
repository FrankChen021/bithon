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

package org.bithon.agent.plugin.spring.webflux.gateway.aop;

import org.bithon.agent.bootstrap.aop.advice.IAdviceAopTemplate;
import org.bithon.agent.core.aop.AopClassHelper;
import org.bithon.agent.core.aop.DynamicInterceptorInstaller;
import org.bithon.agent.core.aop.matcher.Matchers;
import org.bithon.agent.core.config.ConfigurationProperties;
import org.bithon.agent.core.plugin.PluginConfigurationManager;
import org.bithon.agent.plugin.spring.webflux.SpringWebFluxPlugin;
import org.springframework.util.CollectionUtils;
import shaded.net.bytebuddy.asm.Advice;
import shaded.net.bytebuddy.description.method.MethodDescription;
import shaded.net.bytebuddy.dynamic.ClassFileLocator;
import shaded.net.bytebuddy.dynamic.DynamicType;
import shaded.net.bytebuddy.matcher.ElementMatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Frank Chen
 * @date 30/12/21 3:34 PM
 */
public class GatewayAopDynamicInstaller {

    private static Advice aroundFilterAdvice;
    private static Advice beforeFilterAdvice;

    private static final GatewayFilterConfig FILTER_CONFIG;

    static {
        FILTER_CONFIG = PluginConfigurationManager.load(SpringWebFluxPlugin.class).getConfig(GatewayFilterConfig.class);
        if (!CollectionUtils.isEmpty(FILTER_CONFIG)) {
            DynamicType.Unloaded<?> aroundFilterAop = AopClassHelper.generateAopClass(IAdviceAopTemplate.class,
                                                                                      GatewayAopDynamicInstaller.class.getPackage().getName()
                                                                                      + ".AroundFilterAop",
                                                                                      AroundGatewayFilterInterceptor.class.getName(),
                                                                                      true);

            DynamicType.Unloaded<?> beforeFilterAop = AopClassHelper.generateAopClass(IAdviceAopTemplate.class,
                                                                                      GatewayAopDynamicInstaller.class.getPackage().getName()
                                                                                      + ".BeforeFilterAop",
                                                                                      BeforeGatewayFilterInterceptor.class.getName(),
                                                                                      true);

            AopClassHelper.inject(aroundFilterAop, beforeFilterAop);

            aroundFilterAdvice = Advice.to(aroundFilterAop.getTypeDescription(), ClassFileLocator.Simple.of(aroundFilterAop));
            beforeFilterAdvice = Advice.to(beforeFilterAop.getTypeDescription(), ClassFileLocator.Simple.of(beforeFilterAop));
        }
    }

    public static void install(Stream<Class<?>> filters) {
        if (CollectionUtils.isEmpty(FILTER_CONFIG)) {
            return;
        }

        ElementMatcher<? super MethodDescription> filterMethod = Matchers.withName("filter")
                                                                         .and(Matchers.createArgumentsMatcher(false,
                                                                                                              "org.springframework.web.server.ServerWebExchange",
                                                                                                              "org.springframework.cloud.gateway.filter.GatewayFilterChain"));

        Map<String, DynamicInterceptorInstaller.AopDescriptor> descriptors =
            filters.filter(f -> FILTER_CONFIG.containsKey(f.getName()))
                   .map(f -> new DynamicInterceptorInstaller.AopDescriptor(f.getName(),
                                                                           getAdviceClass(f),
                                                                           filterMethod))
                   .filter(f -> f.getAdvice() != null)
                   .collect(Collectors.toMap(DynamicInterceptorInstaller.AopDescriptor::getTargetClass, v -> v));

        DynamicInterceptorInstaller.install(descriptors);
    }

    private static Advice getAdviceClass(Class<?> filterClass) {
        String mode = FILTER_CONFIG.get(filterClass.getName());
        if ("before".equals(mode)) {
            return beforeFilterAdvice;
        } else if ("around".equals(mode)) {
            return aroundFilterAdvice;
        } else {
            return null;
        }
    }

    @ConfigurationProperties(prefix = "agent.plugin.spring.webflux.gateway")
    public static class GatewayFilterConfig extends HashMap<String, String> {
    }
}
