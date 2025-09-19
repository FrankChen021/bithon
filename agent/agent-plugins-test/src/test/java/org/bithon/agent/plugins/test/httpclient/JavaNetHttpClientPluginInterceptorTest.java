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
import org.bithon.agent.plugin.httpclient.javanethttp.JavaNetHttpClientPlugin;
import org.bithon.agent.plugins.test.AbstractPluginInterceptorTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.List;

/**
 * Test case for Java Net HTTP Client plugin (JDK 11+)
 * <p>
 * This test only runs on JDK 11 and above since java.net.http.HttpClient
 * was introduced in Java 11. The plugin itself uses JdkVersionPrecondition.gte(11)
 * to ensure it only loads on compatible JDK versions.
 * <p>
 * This test creates a real HttpClient instance to trigger the loading of
 * internal JDK classes that the plugin instruments.
 * <p>
 * Maven profiles are configured to exclude this test on JDK versions below 11.
 *
 * @author frankchen
 */
public class JavaNetHttpClientPluginInterceptorTest extends AbstractPluginInterceptorTest {

    @Override
    protected IPlugin[] getPlugins() {
        return new IPlugin[]{new JavaNetHttpClientPlugin()};
    }

    @Override
    protected void attemptClassLoading(List<String> classNames) {
        HttpClient client = HttpClient.newHttpClient();
        
        // Create a simple request to further trigger class loading
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://httpbin.org/get"))
            .GET()
            .build();
    }
}
