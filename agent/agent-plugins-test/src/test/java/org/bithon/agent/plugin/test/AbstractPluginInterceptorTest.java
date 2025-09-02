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

package org.bithon.agent.plugin.test;

import com.google.common.collect.ImmutableMap;
import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.configuration.source.Helper;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorManager;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorSupplier;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.Descriptors;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodPointCutDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.installer.InterceptorInstaller;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.PluginResolver;
import org.bithon.agent.instrumentation.loader.PluginClassLoader;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.shaded.net.bytebuddy.agent.ByteBuddyAgent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.io.File;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author frankchen
 */
public abstract class AbstractPluginInterceptorTest {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AbstractPluginInterceptorTest.class);
    private static boolean frameworkInitialized = false;

    /**
     * Initialize the test framework once for all tests.
     * This sets up configuration, class loaders, and installs ByteBuddy agent.
     */
    @BeforeAll
    public static synchronized void initializeFramework() {
        if (frameworkInitialized) {
            return;
        }

        try (MockedStatic<Helper> configurationMock = Mockito.mockStatic(Helper.class)) {
            configurationMock.when(Helper::getCommandLineInputArgs)
                             .thenReturn(Arrays.asList("-Dbithon.application.name=test",
                                                       "-Dbithon.application.env=local",
                                                       "-Dbithon.application.port=9897"
                             ));

            configurationMock.when(Helper::getEnvironmentVariables)
                             .thenReturn(ImmutableMap.of("bithon_t", "t1",
                                                         //Overwrite the prop2
                                                         "bithon_test_prop2", "from_env"));

            ConfigurationManager.createForTesting(new File("not-exists"));
        }

        PluginClassLoader.setClassLoader(AbstractPluginInterceptorTest.class.getClassLoader());

        // Install ByteBuddy agent
        ByteBuddyAgent.install();

        frameworkInitialized = true;
    }

    /**
     * Install interceptors for a given plugin and verify installation.
     *
     * @param plugin The plugin to install interceptors for
     * @return List of interceptor class names that were installed
     */
    protected static List<String> installBeforeClassLoading(IPlugin plugin) {
        // Resolve interceptors
        Descriptors descriptors = new Descriptors();
        descriptors.merge(plugin.getClass().getSimpleName(),
                          plugin.getPreconditions(),
                          plugin.getInterceptors());
        PluginResolver.resolveInterceptorType(descriptors.getAllDescriptor(), Collections.emptyMap());

        // Install interceptors
        new InterceptorInstaller(descriptors)
            .installOn(ByteBuddyAgent.getInstrumentation());

        // Extract all interceptor class names for verification
        return plugin.getInterceptors()
                     .stream()
                     .flatMap(desc -> Arrays.stream(desc.getMethodPointCutDescriptors()))
                     .map(MethodPointCutDescriptor::getInterceptorClassName)
                     .distinct()
                     .collect(Collectors.toList());
    }

    /**
     * Verify that an interceptor is properly installed in the InterceptorManager.
     *
     * @param interceptorClassName The fully qualified class name of the interceptor
     */
    protected static void verifyInterceptorInstalled(String interceptorClassName) {
        Map<String, InterceptorSupplier> suppliers =
            InterceptorManager.INSTANCE.getSuppliers(interceptorClassName);

        // Debug output to understand what's happening
        System.out.println("Checking interceptor: " + interceptorClassName);
        System.out.println("Found suppliers: " + suppliers.size());

        Assertions.assertFalse(suppliers.isEmpty(),
                               "Interceptor " + interceptorClassName + " should be installed. Found " + suppliers.size() + " suppliers.");
    }

    /**
     * Verify that all interceptors for a plugin are properly installed.
     *
     * @param interceptorClassNames List of interceptor class names to verify
     */
    protected static void verifyAllInterceptorsInstalled(List<String> interceptorClassNames) {
        for (String interceptorClassName : interceptorClassNames) {
            verifyInterceptorInstalled(interceptorClassName);
        }
    }

    /**
     * Verify basic plugin structure and configuration.
     *
     * @param plugin                   The plugin to verify
     * @param expectedInterceptorCount Expected number of interceptor descriptors
     */
    protected static void verifyPluginDefinition(IPlugin plugin, int expectedInterceptorCount) {
        List<InterceptorDescriptor> interceptors = plugin.getInterceptors();
        Assertions.assertNotNull(interceptors, "Plugin should define interceptors");
        Assertions.assertEquals(expectedInterceptorCount, interceptors.size(),
                                "Plugin should define exactly " + expectedInterceptorCount + " interceptors");

        // Verify each interceptor has valid configuration
        for (InterceptorDescriptor descriptor : interceptors) {
            Assertions.assertNotNull(descriptor.getTargetClass(),
                                     "Interceptor should have a target class");
            Assertions.assertTrue(descriptor.getMethodPointCutDescriptors().length > 0,
                                  "Interceptor should have at least one method point cut");

            // Verify each method point cut has valid interceptor class name
            for (MethodPointCutDescriptor methodDesc : descriptor.getMethodPointCutDescriptors()) {
                Assertions.assertNotNull(methodDesc.getInterceptorClassName(),
                                         "Method point cut should have interceptor class name");
                Assertions.assertFalse(methodDesc.getInterceptorClassName().isEmpty(),
                                       "Interceptor class name should not be empty");
            }
        }
    }

    /**
     * Attempt to load a target class and verify it can be found.
     * This simulates what happens when the target application loads classes at runtime.
     *
     */
    protected static void attemptClassLoading(List<String> classNames) {
        for (String clazzName : classNames) {
            LoggerFactory.getLogger(AbstractPluginInterceptorTest.class).info("Loading class: {}", clazzName);
            try {
                Class<?> clazz = Class.forName(clazzName);
                Assertions.assertNotNull(clazz, "Class " + clazzName + " should be loadable");
                CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
                log.info("Loaded class {} from {}", clazzName, codeSource == null ? "<unknown>" : codeSource.getLocation());
            } catch (ClassNotFoundException e) {
                Assertions.fail("Class " + clazzName + " not found");
            }
        }
    }

    /**
     * Verify that a method exists on a class with expected signature.
     *
     * @param clazz          The class to check
     * @param methodName     The method name
     * @param parameterTypes Expected parameter types
     */
    protected static void verifyMethodExists(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            clazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            Assertions.fail("Method " + methodName + " not found on class " + clazz.getName() +
                            " - interceptor may need updating");
        }
    }

    @Test
    public void testInterceptorInstallation() {
        IPlugin plugin = getPlugin();

        // Resolve interceptors
        Descriptors descriptors = new Descriptors();
        descriptors.merge(plugin.getClass().getSimpleName(),
                          plugin.getPreconditions(),
                          plugin.getInterceptors());
        PluginResolver.resolveInterceptorType(descriptors.getAllDescriptor(), Collections.emptyMap());

        // Install interceptors
        new InterceptorInstaller(descriptors)
            .installOn(ByteBuddyAgent.getInstrumentation());

        List<String> targetClass = plugin.getInterceptors()
                                         .stream()
                                         .map(InterceptorDescriptor::getTargetClass)
                                         .distinct()
                                         .collect(Collectors.toList());
        Assertions.assertNotEquals(0, targetClass.size());
        // Load class to trigger interceptor to be loaded
        attemptClassLoading(targetClass);

        // Extract all interceptor class names for verification
        List<String> installedInterceptors = plugin.getInterceptors()
                                                   .stream()
                                                   .flatMap(desc -> Arrays.stream(desc.getMethodPointCutDescriptors()))
                                                   .map(MethodPointCutDescriptor::getInterceptorClassName)
                                                   .distinct()
                                                   .collect(Collectors.toList());
        verifyAllInterceptorsInstalled(installedInterceptors);
    }

    protected abstract IPlugin getPlugin();
}
