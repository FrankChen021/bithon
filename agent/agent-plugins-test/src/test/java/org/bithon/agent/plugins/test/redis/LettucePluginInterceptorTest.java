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

import org.bithon.agent.instrumentation.aop.interceptor.installer.InstallerRecorder;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.plugin.redis.lettuce.LettucePlugin;
import org.bithon.agent.plugins.test.AbstractPluginInterceptorTest;
import org.bithon.agent.plugins.test.MavenArtifact;
import org.bithon.agent.plugins.test.MavenArtifactClassLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Test case for Lettuce 5.x plugin
 *
 * @author frankchen
 */
public class LettucePluginInterceptorTest extends AbstractPluginInterceptorTest {
    @Override
    protected IPlugin[] getPlugins() {
        return new IPlugin[]{new LettucePlugin()};
    }

    @Override
    protected ClassLoader getCustomClassLoader() {
        return MavenArtifactClassLoader.create(
            MavenArtifact.of("io.lettuce",
                             "lettuce-core",
                             "6.3.0.RELEASE"),

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
                             "2.7.18"),
            MavenArtifact.of("org.springframework.data",
                             "spring-data-redis",
                             "2.7.18"),
            MavenArtifact.of("org.springframework",
                             "spring-tx",
                             "5.3.13")
        );
    }

    @Test
    @Override
    public void testInterceptorInstallation() {
        super.testInterceptorInstallation();

        // Verify if methods as below are instrumented on target class: org.springframework.data.redis.connection.lettuce.LettuceConnection
        String[] commandMethods = new String[]{
            "geoCommands",
            "hashCommands",
            "hyperLogLogCommands",
            "keyCommands",
            "listCommands",
            "scriptingCommands",
            "setCommands",
            "serverCommands",
            "streamCommands",
            "stringCommands",
            "zSetCommands"
        };
        List<InstallerRecorder.InstrumentedMethod> instrumentedMethods = InstallerRecorder.INSTANCE.getInstrumentedMethods()
                                                                                                   .stream()
                                                                                                   .filter(instrumentedMethod -> "org.springframework.data.redis.connection.lettuce.LettuceConnection".equals(instrumentedMethod.getType()))
                                                                                                   .collect(Collectors.toList());

        for (String commandMethod : commandMethods) {
            Assertions.assertTrue(instrumentedMethods.stream()
                                                     .anyMatch((m) -> commandMethod.equals(m.getMethodName())),
                                  "Method " + commandMethod + " is not instrumented");
        }
    }
}
