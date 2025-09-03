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

package org.bithon.agent.plugins.test.redis;

import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.plugin.redis.lettuce.LettucePlugin;
import org.bithon.agent.plugins.test.AbstractPluginInterceptorTest;
import org.bithon.agent.plugins.test.MavenArtifact;
import org.bithon.agent.plugins.test.MavenArtifactClassLoader;

/**
 * Test case for Lettuce 5.x plugin
 *
 * @author frankchen
 */
public class LettucePluginInterceptorTest extends AbstractPluginInterceptorTest {
    @Override
    protected IPlugin getPlugin() {
        return new LettucePlugin();
    }

    @Override
    protected ClassLoader getCustomClassLoader() {
        return MavenArtifactClassLoader.create(
            MavenArtifact.of("io.lettuce",
                             "lettuce-core",
                             "6.8.0.RELEASE"),

            MavenArtifact.of("io.netty",
                             "netty-transport",
                             "4.1.71.Final"),
            MavenArtifact.of("io.netty",
                             "netty-common",
                             "4.1.71.Final"),
            MavenArtifact.of("io.netty",
                             "netty-buffer",
                             "4.1.71.Final"),

            MavenArtifact.of("org.reactivestreams",
                             "reactive-streams",
                             "1.0.3"),
            MavenArtifact.of("io.projectreactor",
                             "reactor-core",
                             "3.4.10"),
            MavenArtifact.of("org.springframework",
                             "spring-core",
                             "5.3.13"),
            MavenArtifact.of("org.springframework.data",
                             "spring-data-commons",
                             "3.3.13"),
            MavenArtifact.of("org.springframework.data",
                             "spring-data-redis",
                             "3.3.13"),
            MavenArtifact.of("org.springframework",
                             "spring-tx",
                             "5.3.13")
        );
    }
}
