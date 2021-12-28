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

package org.bithon.agent.plugin.spring.webflux.gateway;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.core.aop.AopDebugger;
import org.bithon.agent.core.aop.InstrumentationHelper;
import org.bithon.agent.core.aop.matcher.Matchers;
import org.bithon.agent.core.utils.bytecode.ByteCodeUtils;
import org.bithon.agent.plugin.spring.webflux.gateway.aop.BeanMethodInterceptorFactory;
import org.bithon.agent.plugin.spring.webflux.gateway.aop.FilterMethodAop;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;
import shaded.net.bytebuddy.asm.Advice;
import shaded.net.bytebuddy.description.method.MethodDescription;
import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.dynamic.DynamicType;
import shaded.net.bytebuddy.dynamic.loading.ClassInjector;
import shaded.net.bytebuddy.matcher.ElementMatcher;
import shaded.net.bytebuddy.matcher.ElementMatchers;
import shaded.net.bytebuddy.utility.JavaModule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link org.springframework.cloud.gateway.handler.FilteringWebHandler#FilteringWebHandler(List)}
 * <p>
 * install interceptor for global filter during runtime
 *
 * @author Frank Chen
 * @date 27/12/21 5:31 PM
 */
public class FilteringWebHandler$Ctor extends AbstractInterceptor {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(FilteringWebHandler$Ctor.class);

    @Override
    public boolean initialize() {
        ClassInjector.UsingUnsafe.Factory.resolve(InstrumentationHelper.getInstance()).make(null, null).injectRaw(new HashMap<String, byte[]>() {
            {
                put(BeanMethodInterceptorFactory.class.getName(),
                    ByteCodeUtils.getClassByteCode(BeanMethodInterceptorFactory.class.getName(), BeanMethodInterceptorFactory.class.getClassLoader()));
            }
        });

        return true;
    }

    @Override
    public void onConstruct(AopContext aopContext) {
        if (!(aopContext.getArgs()[0] instanceof List)) {
            return;
        }

        List<GlobalFilter> globalFilters = aopContext.getArgAs(0);


        Set<String> names = globalFilters.stream()
                                         .map(filter -> filter.getClass().getName())
                                         .collect(Collectors.toSet());

        ElementMatcher<? super MethodDescription> filterMethod = Matchers.withName("filter")
                                                                         .and(Matchers.createArgumentsMatcher(false,
                                                                                                              "org.springframework.web.server.ServerWebExchange",
                                                                                                              "org.springframework.cloud.gateway.filter.GatewayFilterChain"));

        Set<String> classes = new HashSet<>();
        classes.add("org.springframework.cloud.gateway.filter.WebClientHttpRoutingFilter");
        classes.add("org.springframework.cloud.gateway.filter.NettyRoutingFilter");
        new AgentBuilder.Default().ignore(ElementMatchers.nameStartsWith("shaded."))
                                  .disableClassFormatChanges()
                                  .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                                  .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                                  .type(Matchers.withNames(names))
                                  .transform((DynamicType.Builder<?> builder, TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule) -> {
                                      if (classes.contains(typeDescription.getTypeName())) {
                                          LOG.info("Installing interceptor to Spring gateway filter [{}]", typeDescription.getTypeName());

                                          return builder.visit(Advice.to(FilterMethodAop.class)
                                                                     .on(filterMethod));
                                      }
                                      return builder;
                                  })
                                  .with(new AopDebugger(names))
                                  .installOn(InstrumentationHelper.getInstance());
    }
}
