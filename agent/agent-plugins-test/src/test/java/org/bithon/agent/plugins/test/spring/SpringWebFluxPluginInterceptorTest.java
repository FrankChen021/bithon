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

import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.plugin.spring.webflux.SpringWebFluxPlugin;
import org.bithon.agent.plugins.test.AbstractPluginInterceptorTest;
import org.bithon.agent.plugins.test.MavenArtifact;
import org.bithon.agent.plugins.test.MavenArtifactClassLoader;

/**
 * Test case for Spring WebFlux plugin
 *
 * @author frankchen
 */
public class SpringWebFluxPluginInterceptorTest extends AbstractPluginInterceptorTest {
    @Override
    protected IPlugin[] getPlugins() {
        return new IPlugin[]{new SpringWebFluxPlugin()};
    }

    @Override
    protected ClassLoader getCustomClassLoader() {
        return MavenArtifactClassLoader.create(
            MavenArtifact.of("org.springframework.cloud",
                             "spring-cloud-gateway-server",
                             "3.1.10"),
            MavenArtifact.of("org.springframework",
                             "spring-webflux",
                             "5.3.39"),
            MavenArtifact.of("io.projectreactor.netty",
                             "reactor-netty-http",
                             "1.0.39"),
            MavenArtifact.of("io.projectreactor.netty",
                             "reactor-netty-core",
                             "1.0.39"),
            MavenArtifact.of("io.projectreactor",
                             "reactor-core",
                             "3.4.10"),
            MavenArtifact.of("org.reactivestreams",
                             "reactive-streams",
                             "1.0.3"),
            MavenArtifact.of("org.springframework",
                             "spring-web",
                             "5.3.0"),
            MavenArtifact.of("io.netty",
                             "netty-all",
                             "4.1.63.Final")
        );
    }
}
