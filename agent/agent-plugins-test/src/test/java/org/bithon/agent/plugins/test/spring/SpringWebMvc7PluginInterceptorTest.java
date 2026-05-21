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

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.plugin.spring.webmvc.SpringWebMvcPlugin;
import org.bithon.agent.plugin.spring.webmvc7.SpringWebMvc7Plugin;
import org.bithon.agent.plugins.test.AbstractPluginInterceptorTest;
import org.bithon.agent.plugins.test.MavenArtifact;
import org.bithon.agent.plugins.test.MavenArtifactClassLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.util.List;

/**
 * Test case for Spring WebMVC 7 plugin.
 *
 * @author frankchen
 */
@EnabledForJreRange(min = JRE.JAVA_17)
public class SpringWebMvc7PluginInterceptorTest extends AbstractPluginInterceptorTest {
    @Override
    protected IPlugin[] getPlugins() {
        return new IPlugin[]{new SpringWebMvcPlugin(), new SpringWebMvc7Plugin()};
    }

    @Override
    protected ClassLoader getCustomClassLoader() {
        return MavenArtifactClassLoader.create(
            MavenArtifact.of("org.springframework",
                             "spring-webmvc",
                             "7.0.1"),
            MavenArtifact.of("org.springframework",
                             "spring-context",
                             "7.0.1"),
            MavenArtifact.of("org.springframework",
                             "spring-core",
                             "7.0.1"),
            MavenArtifact.of("org.springframework",
                             "spring-beans",
                             "7.0.1"),
            MavenArtifact.of("org.springframework",
                             "spring-web",
                             "7.0.1"),
            MavenArtifact.of("io.micrometer",
                             "micrometer-observation",
                             "1.16.0"),
            MavenArtifact.of("io.micrometer",
                             "micrometer-commons",
                             "1.16.0"),
            MavenArtifact.of("commons-logging",
                             "commons-logging",
                             "1.3.5"),
            MavenArtifact.of("org.jspecify",
                             "jspecify",
                             "1.0.0")
        );
    }

    @Test
    public void legacyPluginDoesNotMatchSpring7() {
        ClassLoader classLoader = getCustomClassLoader();

        for (InterceptorDescriptor descriptor : new SpringWebMvcPlugin().getInterceptors()) {
            Assertions.assertNotNull(descriptor.getPrecondition());
            Assertions.assertFalse(descriptor.getPrecondition().matches(classLoader, null),
                                   descriptor.getTargetClass() + " should be skipped for Spring 7");
        }

        List<InterceptorDescriptor> spring7Descriptors = new SpringWebMvc7Plugin().getInterceptors();
        Assertions.assertEquals(1, spring7Descriptors.size());
        Assertions.assertEquals("org.springframework.web.method.support.InvocableHandlerMethod",
                                spring7Descriptors.get(0).getTargetClass());

        for (InterceptorDescriptor descriptor : spring7Descriptors) {
            Assertions.assertNotNull(descriptor.getPrecondition());
            Assertions.assertTrue(descriptor.getPrecondition().matches(classLoader, null),
                                  descriptor.getTargetClass() + " should match Spring 7");
        }
    }

    @Test
    public void versionPreconditionsAreScopedToClassLoader() {
        ClassLoader spring4ClassLoader = createSpring4ClassLoader();
        ClassLoader spring7ClassLoader = getCustomClassLoader();

        InterceptorDescriptor legacyDescriptor = new SpringWebMvcPlugin().getInterceptors().get(0);
        Assertions.assertFalse(legacyDescriptor.getPrecondition().matches(spring7ClassLoader, null));
        Assertions.assertTrue(legacyDescriptor.getPrecondition().matches(spring4ClassLoader, null));

        InterceptorDescriptor spring7Descriptor = new SpringWebMvc7Plugin().getInterceptors().get(0);
        Assertions.assertFalse(spring7Descriptor.getPrecondition().matches(spring4ClassLoader, null));
        Assertions.assertTrue(spring7Descriptor.getPrecondition().matches(spring7ClassLoader, null));
    }

    private static ClassLoader createSpring4ClassLoader() {
        return MavenArtifactClassLoader.create(
            MavenArtifact.of("org.springframework",
                             "spring-webmvc",
                             "4.3.12.RELEASE"),
            MavenArtifact.of("org.springframework",
                             "spring-core",
                             "4.3.12.RELEASE"),
            MavenArtifact.of("org.springframework",
                             "spring-web",
                             "4.3.12.RELEASE")
        );
    }
}
