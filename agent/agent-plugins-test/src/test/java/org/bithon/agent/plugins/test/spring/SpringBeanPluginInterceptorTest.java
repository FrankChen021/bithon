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
import org.bithon.agent.plugin.spring.bean.SpringBeanPlugin;
import org.bithon.agent.plugins.test.AbstractPluginInterceptorTest;
import org.bithon.agent.plugins.test.MavenArtifact;
import org.bithon.agent.plugins.test.MavenArtifactClassLoader;

/**
 * Test case for Spring Bean plugin
 *
 * @author frankchen
 */
public class SpringBeanPluginInterceptorTest extends AbstractPluginInterceptorTest {
    @Override
    protected IPlugin[] getPlugins() {
        return new IPlugin[]{new SpringBeanPlugin()};
    }

    @Override
    protected ClassLoader getCustomClassLoader() {
        return MavenArtifactClassLoader.create(
            MavenArtifact.of("org.springframework",
                             "spring-context",
                             "4.3.12.RELEASE"),
            MavenArtifact.of("org.springframework",
                             "spring-core",
                             "4.3.12.RELEASE"),
            MavenArtifact.of("org.springframework",
                             "spring-beans",
                             "4.3.12.RELEASE")
        );
    }
}
