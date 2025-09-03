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

package org.bithon.agent.plugins.test.apache.druid;

import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.plugin.apache.druid.ApacheDruidPlugin;
import org.bithon.agent.plugins.test.AbstractPluginInterceptorTest;
import org.bithon.agent.plugins.test.MavenArtifact;
import org.bithon.agent.plugins.test.MavenArtifactClassLoader;

/**
 *
 * @author frankchen
 */
public class DruidPluginInterceptorInterceptorTest extends AbstractPluginInterceptorTest {
    @Override
    protected IPlugin getPlugin() {
        return new ApacheDruidPlugin();
    }

    @Override
    protected ClassLoader getCustomClassLoader() {
        return MavenArtifactClassLoader.create(

            MavenArtifact.of("org.apache.druid",
                             "druid-core",
                             "24.0.0"),
            MavenArtifact.of("org.apache.druid",
                             "druid-processing",
                             "24.0.0"),
            MavenArtifact.of("org.apache.druid",
                             "druid-sql",
                             "24.0.0"),
            MavenArtifact.of("org.apache.druid",
                             "druid-server",
                             "24.0.0"),

            MavenArtifact.of("io.netty",
                             "netty",
                             "3.10.6.Final"),
            MavenArtifact.of("com.fasterxml.jackson.core",
                             "jackson-databind",
                             "2.10.5.1"),
            MavenArtifact.of("com.fasterxml.jackson.core",
                             "jackson-core",
                             "2.10.5"),
            MavenArtifact.of("javax.ws.rs",
                             "jsr311-api",
                             "1.1.1"),
            MavenArtifact.of("javax.servlet",
                             "javax.servlet-api",
                             "3.1.0")
            );
    }
}
