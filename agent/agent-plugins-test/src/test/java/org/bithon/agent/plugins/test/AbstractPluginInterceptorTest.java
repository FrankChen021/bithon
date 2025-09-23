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

package org.bithon.agent.plugins.test;

import com.google.common.collect.ImmutableMap;
import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.configuration.source.Helper;
import org.bithon.agent.instrumentation.aop.InstrumentationHelper;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.Descriptors;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.MethodPointCutDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.installer.InstallerRecorder;
import org.bithon.agent.instrumentation.aop.interceptor.installer.InterceptorInstaller;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.PluginResolver;
import org.bithon.agent.instrumentation.loader.PluginClassLoader;
import org.bithon.agent.observability.event.EventMessage;
import org.bithon.agent.observability.exporter.IMessageConverter;
import org.bithon.agent.observability.exporter.IMessageExporter;
import org.bithon.agent.observability.exporter.IMessageExporterFactory;
import org.bithon.agent.observability.exporter.config.ExporterConfig;
import org.bithon.agent.observability.metric.collector.jvm.JmxBeans;
import org.bithon.agent.observability.metric.domain.jvm.JvmMetrics;
import org.bithon.agent.observability.metric.model.IMeasurement;
import org.bithon.agent.observability.metric.model.schema.Schema;
import org.bithon.agent.observability.metric.model.schema.Schema2;
import org.bithon.agent.observability.metric.model.schema.Schema3;
import org.bithon.agent.observability.tracing.Tracer;
import org.bithon.agent.observability.tracing.context.ITraceSpan;
import org.bithon.agent.observability.tracing.reporter.ITraceReporter;
import org.bithon.agent.observability.tracing.reporter.ReporterConfig;
import org.bithon.shaded.net.bytebuddy.agent.ByteBuddyAgent;
import org.bithon.shaded.net.bytebuddy.agent.builder.AgentBuilder;
import org.bithon.shaded.net.bytebuddy.utility.JavaModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract base class for plugin interceptor tests.
 * <p>
 * This class provides utilities for testing plugin interceptors, including:
 * - Framework initialization and configuration
 * - Interceptor installation and verification
 * - Maven artifact resolution and class loading functionality
 *
 * @author frankchen
 */
public abstract class AbstractPluginInterceptorTest {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AbstractPluginInterceptorTest.class);
    private boolean agentInstalled = false;

    /**
     * Initialize the test framework before each test.
     * This sets up configuration, class loaders, and installs ByteBuddy agent.
     */
    private synchronized void installAgent() {
        if (agentInstalled) {
            return;
        }

        try (MockedStatic<Helper> configurationMock = Mockito.mockStatic(Helper.class)) {
            configurationMock.when(Helper::getCommandLineInputArgs)
                             .thenReturn(Arrays.asList("-Dbithon.application.name=test",
                                                       "-Dbithon.application.env=local",
                                                       "-Dbithon.application.port=9897"
                             ));

            configurationMock.when(Helper::getEnvironmentVariables)
                             .thenReturn(getEnvironmentVariables());

            ConfigurationManager.createForTesting(new File("not-exists"));
        }

        PluginClassLoader.setClassLoader(AbstractPluginInterceptorTest.class.getClassLoader());

        // Install ByteBuddy agent
        ByteBuddyAgent.install();

        agentInstalled = true;
    }

    @BeforeEach
    public final void beforeEachTestCase() {
        installAgent();

        initializeBeforeEachTestCase();
    }

    protected final List<ITraceSpan> reportedSpans = Collections.synchronizedList(new ArrayList<>());
    protected static final List<IMeasurement> REPORTED_METRICS = Collections.synchronizedList(new ArrayList<>());

    public static class TestFactory implements IMessageExporterFactory {
        @Override
        public IMessageExporter createMetricExporter(ExporterConfig exporterConfig) {
            return new IMessageExporter() {
                @Override
                public void export(Object message) {
                    if (message instanceof Collection) {
                        //noinspection unchecked
                        REPORTED_METRICS.addAll((Collection<IMeasurement>) message);
                    } else {
                        REPORTED_METRICS.add((IMeasurement) message);
                    }
                }

                @Override
                public void close() {
                }
            };
        }

        @Override
        public IMessageExporter createTracingExporter(ExporterConfig exporterConfig) {
            return new IMessageExporter() {
                @Override
                public void export(Object message) {
                }

                @Override
                public void close() {
                }
            };
        }

        @Override
        public IMessageExporter createEventExporter(ExporterConfig exporterConfig) {
            return null;
        }

        @Override
        public IMessageConverter createMessageConverter() {
            return new IMessageConverter() {
                @Override
                public Object from(long timestamp, int interval, JvmMetrics metrics) {
                    return null;
                }

                @Override
                public Object from(ITraceSpan span) {
                    return null;
                }

                @Override
                public Object from(EventMessage event) {
                    return null;
                }

                @Override
                public Object from(Map<String, String> log) {
                    return null;
                }

                @Override
                public Object from(Schema schema,
                                   Collection<IMeasurement> measurementList,
                                   long timestamp,
                                   int interval) {
                    return measurementList;
                }

                @Override
                public Object from(Schema2 schema,
                                   Collection<IMeasurement> measurementList,
                                   long timestamp,
                                   int interval) {
                    return measurementList;
                }

                @Override
                public Object from(Schema3 schema, List<Object[]> measurementList, long timestamp, int interval) {
                    return measurementList;
                }
            };
        }
    }

    protected Map<String, String> getEnvironmentVariables() {
        // Add SDK-specific environment variables for testing
        return ImmutableMap.of(
            "bithon_exporters_tracing_client_factory", TestFactory.class.getName(),
            "bithon_exporters_metric_client_factory", TestFactory.class.getName(),
            "bithon_tracing_debug", "true"
        );
    }

    protected void initializeBeforeEachTestCase() {
        reportedSpans.clear();
        REPORTED_METRICS.clear();

        // Replace default report
        Tracer.get()
              .reporter(new ITraceReporter() {
                  @Override
                  public ReporterConfig getReporterConfig() {
                      return new ReporterConfig();
                  }

                  @Override
                  public void report(List<ITraceSpan> spans) {
                      reportedSpans.addAll(spans);
                  }
              });
    }

    protected ClassLoader getCustomClassLoader() {
        return null;
    }

    private ClassLoader customClassLoader;

    /**
     * Attempt to load a target class and verify it can be found.
     * This simulates what happens when the target application loads classes at runtime.
     * Use the custom class loader set via setClassLoader() if available, otherwise use Class.forName().
     *
     */
    protected void attemptClassLoading(List<String> classNames) {
        for (String clazzName : classNames) {
            try {
                log.info("Loading class {} with {}", clazzName,
                         customClassLoader == null ? "system loader" : customClassLoader.getClass().getSimpleName());
                Class<?> clazz = customClassLoader == null ? Class.forName(clazzName) : Class.forName(clazzName, false, customClassLoader);

                Assertions.assertNotNull(clazz, "Class " + clazzName + " should be loadable");
                CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
                log.info("Loaded class {} with {} from {}",
                         clazzName,
                         customClassLoader == null ? "system loader" : customClassLoader.getClass().getSimpleName(),
                         codeSource == null ? "<unknown>" : codeSource.getLocation());
            } catch (ClassNotFoundException e) {
                String classLoaderInfo = customClassLoader != null ?
                                         " using custom class loader: " + customClassLoader.getClass().getSimpleName() :
                                         " using default class loader";
                classLoaderInfo += ". JDK version: " + getCurrentJavaVersion();
                Assertions.fail("Class " + clazzName + " not found" + classLoaderInfo);
            } catch (NoClassDefFoundError error) {
                log.error("Class not found", error);
                List<String> classPath = Arrays.stream(JmxBeans.RUNTIME_BEAN.getClassPath().split(File.pathSeparator))
                                               .sorted()
                                               .collect(Collectors.toList());
                for (String s : classPath) {
                    log.info("{}", s);
                }
                Assertions.fail("Fail to load class " + clazzName + " not found:" + error.getMessage());
            }
        }
    }

    private static int getCurrentJavaVersion() {
        String specVersion = ManagementFactory.getRuntimeMXBean().getSpecVersion();
        String[] versionParts = specVersion.split("\\.");
        if (versionParts[0].equals("1")) {
            // For Java 1.x (e.g., 1.8)
            return Integer.parseInt(versionParts[1]);
        } else {
            // For Java 9 and above
            return Integer.parseInt(versionParts[0]);
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
    @Execution(ExecutionMode.SAME_THREAD)
    public void testInterceptorInstallation() {
        this.customClassLoader = getCustomClassLoader();

        IPlugin[] plugins = getPlugins();

        // Resolve interceptors
        Descriptors descriptors = new Descriptors();
        for (IPlugin plugin : plugins) {
            descriptors.merge(plugin.getClass().getSimpleName(),
                              plugin.getPreconditions(),
                              plugin.getInterceptors());
        }
        PluginResolver.resolveInterceptorType(descriptors.getAllDescriptor(), Collections.emptyMap());

        InstrumentationHelper.setErrorHandler(new AgentBuilder.Listener.Adapter() {
            @Override
            public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                log.error("Fail to instrument class {}", typeName, throwable);
            }
        });

        // Install interceptors
        new InterceptorInstaller(descriptors)
            .installOn(ByteBuddyAgent.getInstrumentation());

        List<String> targetClass = Arrays.stream(plugins)
                                         .flatMap(plugin -> plugin.getInterceptors().stream())
                                         .filter((desc) -> desc.getPrecondition() == null || desc.getPrecondition().matches(this.customClassLoader, null))
                                         .map(InterceptorDescriptor::getTargetClass)
                                         .distinct()
                                         .collect(Collectors.toList());
        Assertions.assertNotEquals(0, targetClass.size());
        // Load class to trigger interceptor to be loaded
        attemptClassLoading(targetClass);

        // Extract all interceptor class names for verification
        List<String> installedInterceptors = Arrays.stream(plugins)
                                                   .flatMap(plugin -> plugin.getInterceptors().stream())
                                                   .filter((desc) -> desc.getPrecondition() == null || desc.getPrecondition().matches(this.customClassLoader, null))
                                                   .flatMap(desc -> Arrays.stream(desc.getMethodPointCutDescriptors()))
                                                   .map(MethodPointCutDescriptor::getInterceptorClassName)
                                                   .distinct()
                                                   .collect(Collectors.toList());

        // Additional verification: Check that interceptors are actually applied to methods
        // This uses InstallerRecorder which only records methods that were actually matched and instrumented
        verifyMethodsActuallyInstrumented(installedInterceptors);
    }

    /**
     * Verify that interceptors are actually applied to methods by checking InstallerRecorder.
     * This is more accurate than just checking InterceptorManager because InstallerRecorder
     * only records methods that were actually matched and instrumented by ByteBuddy.
     *
     * @param expectedInterceptors List of interceptor class names that should have been applied
     */
    protected static void verifyMethodsActuallyInstrumented(List<String> expectedInterceptors) {
        List<InstallerRecorder.InstrumentedMethod> instrumentedMethods = InstallerRecorder.INSTANCE.getInstrumentedMethods();

        log.info("InstallerRecorder found {} actually instrumented methods:", instrumentedMethods.size());

        // Verify that each expected interceptor has at least one instrumented method
        for (String expectedInterceptor : expectedInterceptors) {
            boolean found = instrumentedMethods.stream()
                                               .anyMatch(method -> expectedInterceptor.equals(method.getInterceptorName()));

            if (!found) {
                log.warn("Interceptor {} has no instrumented methods recorded in InstallerRecorder. " +
                         "Instrumented methods: {}",
                         expectedInterceptor,
                         instrumentedMethods.stream()
                                            .map(InstallerRecorder.InstrumentedMethod::toString)
                                            .collect(Collectors.joining("\n")));
            }
            Assertions.assertTrue(found,
                                  "Interceptor " + expectedInterceptor + " should have at least one instrumented method. " +
                                  "This indicates the method signature matching failed.");
        }
    }

    protected abstract IPlugin[] getPlugins();
}
