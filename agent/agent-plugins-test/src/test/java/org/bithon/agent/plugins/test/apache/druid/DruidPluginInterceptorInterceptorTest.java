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

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodPointCutDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.plugin.apache.druid.ApacheDruidPlugin;
import org.bithon.agent.plugins.test.AbstractPluginInterceptorTest;
import org.bithon.agent.plugins.test.MavenArtifact;
import org.bithon.agent.plugins.test.MavenArtifactClassLoader;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author frankchen
 */
public class DruidPluginInterceptorInterceptorTest extends AbstractPluginInterceptorTest {
    @Override
    protected IPlugin[] getPlugins() {
        return new IPlugin[]{new ApacheDruidPlugin()};
    }

    @Override
    protected ClassLoader getCustomClassLoader() {
        return createDruidClassLoader("24.0.0");
    }

    @Test
    public void sqlResourceDoPostPointcutDoesNotMatchJerseyEntrypoint() throws Exception {
        Class<?> sqlResource = loadDruid34SqlResourceOrSkip();

        MethodPointCutDescriptor doPostPointcut = getSqlResourceDoPostPointcut();
        List<Method> matchedMethods = Arrays.stream(sqlResource.getDeclaredMethods())
                                            .filter((method) -> "doPost".equals(method.getName()))
                                            .filter((method) -> doPostPointcut.getMethodMatcher()
                                                                              .matches(new MethodDescription.ForLoadedMethod(method)))
                                            .collect(Collectors.toList());

        Assertions.assertFalse(matchedMethods.isEmpty());
        Assertions.assertTrue(matchedMethods.stream().allMatch(this::isSqlQueryDoPost),
                              matchedMethods.stream()
                                            .map(this::toSignature)
                                            .collect(Collectors.joining("\n")));
    }

    private Class<?> loadDruid34SqlResourceOrSkip() throws ClassNotFoundException {
        try {
            return Class.forName("org.apache.druid.sql.http.SqlResource",
                                 false,
                                 createDruid34ClassLoader());
        } catch (UnsupportedClassVersionError ignored) {
            Assumptions.assumeTrue(false, "Druid 34 requires Java 11+ bytecode support");
            throw ignored;
        }
    }

    private MethodPointCutDescriptor getSqlResourceDoPostPointcut() {
        for (InterceptorDescriptor descriptor : new ApacheDruidPlugin().getInterceptors()) {
            if ("org.apache.druid.sql.http.SqlResource".equals(descriptor.getTargetClass())) {
                return descriptor.getMethodPointCutDescriptors()[0];
            }
        }

        throw new AssertionError("SqlResource#doPost pointcut not found");
    }

    private boolean isSqlQueryDoPost(Method method) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        return parameterTypes.length == 2
               && "org.apache.druid.sql.http.SqlQuery".equals(parameterTypes[0].getName())
               && "javax.servlet.http.HttpServletRequest".equals(parameterTypes[1].getName());
    }

    private String toSignature(Method method) {
        return method.getName() + "("
               + Arrays.stream(method.getParameterTypes())
                       .map(Class::getName)
                       .collect(Collectors.joining(", "))
               + ")";
    }

    private ClassLoader createDruidClassLoader(String druidVersion) {
        return MavenArtifactClassLoader.create(

            MavenArtifact.of("org.apache.druid",
                             "druid-core",
                             druidVersion),
            MavenArtifact.of("org.apache.druid",
                             "druid-processing",
                             druidVersion),
            MavenArtifact.of("org.apache.druid",
                             "druid-sql",
                             druidVersion),
            MavenArtifact.of("org.apache.druid",
                             "druid-server",
                             druidVersion),

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
                             "3.1.0"),
            MavenArtifact.of("com.sun.jersey",
                             "jersey-core",
                             "1.19.4"),
            MavenArtifact.of("com.sun.jersey",
                             "jersey-server",
                             "1.19.4")
        );
    }

    private ClassLoader createDruid34ClassLoader() {
        return MavenArtifactClassLoader.create(

            MavenArtifact.of("org.apache.druid",
                             "druid-processing",
                             "34.0.0"),
            MavenArtifact.of("org.apache.druid",
                             "druid-sql",
                             "34.0.0"),
            MavenArtifact.of("org.apache.druid",
                             "druid-server",
                             "34.0.0"),

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
                             "3.1.0"),
            MavenArtifact.of("com.sun.jersey",
                             "jersey-core",
                             "1.19.4"),
            MavenArtifact.of("com.sun.jersey",
                             "jersey-server",
                             "1.19.4")
        );
    }
}
