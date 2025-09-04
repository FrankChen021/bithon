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

package org.bithon.agent.plugins.test.httpclient;

import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.plugin.httpclient.okhttp32.OkHttp32HttpClientPlugin;
import org.bithon.agent.plugins.test.AbstractPluginInterceptorTest;
import org.bithon.agent.plugins.test.MavenArtifact;
import org.bithon.agent.plugins.test.MavenArtifactClassLoader;

/**
 * Test case for OkHttp 3.2 plugin
 *
 * @author frankchen
 */
public class OkHttp32PluginInterceptorTest extends AbstractPluginInterceptorTest {
    @Override
    protected IPlugin[] getPlugins() {
        return new IPlugin[]{new OkHttp32HttpClientPlugin()};
    }

    @Override
    protected ClassLoader getCustomClassLoader() {
        return MavenArtifactClassLoader.create(
            MavenArtifact.of("com.squareup.okhttp3",
                             "okhttp",
                             "3.2.0"),
            MavenArtifact.of("com.squareup.okio",
                             "okio",
                             "2.5.0"),
            MavenArtifact.of("com.squareup.okhttp3",
                             "okhttp",
                             "4.4.0"));
    }
}
