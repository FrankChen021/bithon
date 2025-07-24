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

package org.bithon.agent.instrumentation.aop.debug;


import org.bithon.agent.instrumentation.logging.ILogger;
import org.bithon.agent.instrumentation.logging.LoggerFactory;
import org.bithon.shaded.net.bytebuddy.agent.builder.AgentBuilder;
import org.bithon.shaded.net.bytebuddy.description.type.TypeDescription;
import org.bithon.shaded.net.bytebuddy.dynamic.DynamicType;
import org.bithon.shaded.net.bytebuddy.utility.JavaModule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frankchen
 */
public class Debugger extends AgentBuilder.Listener.Adapter {
    protected static final ILogger log = LoggerFactory.getLogger(Debugger.class);

    private final File classFileDirectory;
    private final DebugConfig debugConfig;
    private final Set<String> types;
    private final Map<String, ProfilingTimestamp> timestamps;

    public Debugger(DebugConfig debugConfig, File classFileDirectory) {
        this.debugConfig = debugConfig;
        this.classFileDirectory = classFileDirectory;

        // Clean up directories before startup.
        // This is convenient for debugging
        try {
            Files.walk(classFileDirectory.toPath())
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        } catch (IOException ignored) {
        }

        try {
            if (!classFileDirectory.exists()) {
                classFileDirectory.mkdirs();
            }
        } catch (Exception e) {
            log.error("log error", e);
        }

        this.types = Collections.emptySet();
        this.timestamps = new ConcurrentHashMap<>();
    }

    private Debugger(DebugConfig debugConfig, File directory, Set<String> types) {
        this.debugConfig = debugConfig;
        this.classFileDirectory = directory;
        this.types = types;
        this.timestamps = new ConcurrentHashMap<>();
    }

    public Debugger withTypes(Set<String> newTargetTypes) {
        return new Debugger(this.debugConfig, this.classFileDirectory, newTargetTypes);
    }

    public void writeTo(String className, byte[] classInBytes) {
        if (!debugConfig.isOutputClassFile()) {
            return;
        }

        try {
            File file = new File(this.classFileDirectory, className + ".class");
            file.getParentFile().mkdirs();
            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write(classInBytes);
            }
        } catch (IOException ignored) {
        }
    }

    public void writeTo(DynamicType dynamicType) {
        if (!debugConfig.isOutputClassFile()) {
            return;
        }
        try {
            log.info("Saving transformed class [{}] to [{}]...", dynamicType.getTypeDescription().getTypeName(), classFileDirectory);
            dynamicType.saveIn(this.classFileDirectory);
        } catch (Throwable e) {
            log.warn("Failed to save transformed class {} to file." + dynamicType.getTypeDescription().getActualName(), e);
        }
    }

    @Override
    public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
        if (debugConfig.isProfiling() && this.types.contains(typeName)) {
            timestamps.computeIfAbsent(typeName, k -> new ProfilingTimestamp())
                .discoveryTimestamp = System.nanoTime();
        }
    }

    @Override
    public void onTransformation(TypeDescription typeDescription,
                                 ClassLoader classLoader,
                                 JavaModule javaModule,
                                 boolean loaded,
                                 DynamicType dynamicType) {
        if (debugConfig.isProfiling()) {
            timestamps.computeIfAbsent(typeDescription.getTypeName(), k -> new ProfilingTimestamp())
                .transformationTimestamp = System.nanoTime();
        }

        if (debugConfig.isOutputClassFile() && this.types.contains(typeDescription.getTypeName())) {
            writeTo(dynamicType);
        }
    }

    @Override
    public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
        if (debugConfig.isProfiling() && this.types.contains(typeName)) {
            ProfilingTimestamp timestmap = timestamps.computeIfAbsent(typeName, k -> new ProfilingTimestamp());
            timestmap.completionTimestamp = System.nanoTime();

            log.info(String.format(Locale.ENGLISH,
                                   "Transformed: %s: discovery->transformation=%d, transformation->completion=%d",
                                   typeName,
                                   timestmap.transformationTimestamp - timestmap.discoveryTimestamp,
                                   timestmap.completionTimestamp - timestmap.transformationTimestamp));
        }
    }

    @Override
    public void onError(String typeName, ClassLoader classLoader, JavaModule javaModule, boolean b, Throwable throwable) {
        log.error(String.format(Locale.ENGLISH, "Failed to transform %s", typeName), throwable);
    }
}
