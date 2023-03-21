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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;

/**
 * @author frankchen
 */
public class AopDebugger extends AgentBuilder.Listener.Adapter {
    protected static final ILogger log = LoggerFactory.getLogger(AopTransformationListener.class);

    private final File classFileDirectory;

    /**
     * corresponding to <b>bithon.aop.debug</b> configuration item
     */
    private final boolean isEnabled;

    private final Set<String> types;

    public AopDebugger(boolean enabled, File directory) {
        isEnabled = enabled;

        classFileDirectory = directory;

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

        types = Collections.emptySet();
    }

    public AopDebugger(boolean enabled, File directory, Set<String> types) {
        this.isEnabled = enabled;
        this.classFileDirectory = directory;
        this.types = types;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public File getClassFileDirectory() {
        return classFileDirectory;
    }

    public AopDebugger withTypes(Set<String> newTargetTypes) {
        return new AopDebugger(this.isEnabled, this.classFileDirectory, newTargetTypes);
    }

    public void saveClazz(DynamicType dynamicType) {
        if (!isEnabled) {
            return;
        }
        try {
            log.info("Saving [{}] to [{}]...", dynamicType.getTypeDescription().getTypeName(), classFileDirectory);
            dynamicType.saveIn(classFileDirectory);
        } catch (Throwable e) {
            log.warn("Failed to save class {} to file." + dynamicType.getTypeDescription().getActualName(), e);
        }
    }

    @Override
    public void onTransformation(TypeDescription typeDescription,
                                 ClassLoader classLoader,
                                 JavaModule javaModule,
                                 boolean loaded, DynamicType dynamicType) {
        if (isEnabled && this.types.contains(typeDescription.getTypeName())) {
            log.info("{} Transformed", typeDescription.getTypeName());
            saveClazz(dynamicType);
        }
    }

    @Override
    public void onError(String s, ClassLoader classLoader, JavaModule javaModule, boolean b, Throwable throwable) {
        log.error(String.format(Locale.ENGLISH, "Failed to transform %s", s), throwable);
    }
}
