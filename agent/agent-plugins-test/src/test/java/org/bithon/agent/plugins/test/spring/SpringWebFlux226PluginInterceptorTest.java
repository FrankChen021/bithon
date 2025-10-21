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

package org.bithon.agent.plugins.test.spring;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.BithonClassDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.precondition.IInterceptorPrecondition;
import org.bithon.agent.plugin.spring.webflux.SpringWebFluxPlugin;
import org.bithon.agent.plugins.test.AbstractPluginInterceptorTest;
import org.bithon.agent.plugins.test.MavenArtifact;
import org.bithon.agent.plugins.test.MavenArtifactClassLoader;

import java.util.List;

/**
 * Test case for Spring WebFlux plugin
 *
 * @author frankchen
 */
public class SpringWebFlux226PluginInterceptorTest extends AbstractPluginInterceptorTest {
    @Override
    protected IPlugin[] getPlugins() {
        IPlugin plugin = new SpringWebFluxPlugin();
        List<InterceptorDescriptor> descriptors = plugin.getInterceptors();
        descriptors.removeIf((descriptor) -> descriptor.getTargetClass().equals("reactor.netty.http.server.HttpServerConfig$HttpServerChannelInitializer"));
        return new IPlugin[]{
            new IPlugin() {
                @Override
                public IInterceptorPrecondition getPreconditions() {
                    return plugin.getPreconditions();
                }

                @Override
                public BithonClassDescriptor getBithonClassDescriptor() {
                    return plugin.getBithonClassDescriptor();
                }

                @Override
                public List<InterceptorDescriptor> getInterceptors() {
                    return descriptors;
                }
            }
        };
    }

    @Override
    protected ClassLoader getCustomClassLoader() {
        return MavenArtifactClassLoader.create(
            MavenArtifact.of("org.springframework.cloud",
                             "spring-cloud-gateway-server",
                             "2.2.6.RELEASE"),
            MavenArtifact.of("org.springframework",
                             "spring-web",
                             "5.2.8.RELEASE"),
            MavenArtifact.of("org.springframework",
                             "spring-webflux",
                             "5.2.8.RELEASE"),

            MavenArtifact.of("io.projectreactor.netty",
                             "reactor-netty",
                             "0.9.10.RELEASE"),

            MavenArtifact.of("io.projectreactor",
                             "reactor-core",
                             "3.3.8.RELEASE"),
            MavenArtifact.of("org.reactivestreams",
                             "reactive-streams",
                             "1.0.3"),

            MavenArtifact.of("io.netty",
                             "netty-all",
                             "4.1.63.Final")
        );
    }
}
