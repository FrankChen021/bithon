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

import org.bithon.agent.plugins.test.MavenArtifactClassLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Same as {@link HttpComponents4PluginInterceptorTest}, but the target classes are loaded by the test class path
 * before the interceptors are installed.
 * <p>
 * This is what happens when the Maven Resolver used by {@link MavenArtifactClassLoader} makes HTTP requests
 * with Apache HttpClient 4 to verify or download artifacts.
 * The classes loaded from the artifacts must still be instrumented.
 */
public class HttpComponents4PreloadedPluginInterceptorTest extends HttpComponents4PluginInterceptorTest {

    private static final List<String> PRELOADED_CLASSES = Arrays.asList(
        "org.apache.http.impl.client.InternalHttpClient",
        "org.apache.http.protocol.HttpRequestExecutor"
    );

    private static ClassLoader getParentClassLoader() {
        return MavenArtifactClassLoader.class.getClassLoader();
    }

    private static void preloadByParent() {
        for (String className : PRELOADED_CLASSES) {
            try {
                Class.forName(className, false, getParentClassLoader());
            } catch (ClassNotFoundException e) {
                Assertions.fail("Class " + className + " should be on the test class path", e);
            }
        }
    }

    @Override
    protected ClassLoader getCustomClassLoader() {
        preloadByParent();
        return super.getCustomClassLoader();
    }

    @Test
    public void testClassesAreDefinedByArtifactClassLoader() throws ClassNotFoundException {
        preloadByParent();

        ClassLoader artifactClassLoader = super.getCustomClassLoader();
        for (String className : PRELOADED_CLASSES) {
            Class<?> parentClass = Class.forName(className, false, getParentClassLoader());
            Class<?> artifactClass = Class.forName(className, false, artifactClassLoader);

            Assertions.assertSame(artifactClassLoader, artifactClass.getClassLoader(), className + " should be defined by the artifact class loader");
            Assertions.assertNotSame(parentClass, artifactClass);
        }
    }
}
