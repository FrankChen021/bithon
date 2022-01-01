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

package org.bithon.agent.core.aop;


import org.bithon.agent.core.context.AgentContext;
import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.dynamic.DynamicType;
import shaded.net.bytebuddy.utility.JavaModule;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;

import static java.io.File.separator;

/**
 * @author frankchen
 */
public class AopDebugger extends AopTransformationListener {
    private static final File CLASS_FILE_DIR;

    /**
     * corresponding to <b>bithon.aop.debug</b> configuration item
     */
    private static final boolean IS_DEBUG_ENABLED;

    static {
        IS_DEBUG_ENABLED = AgentContext.getInstance().getAgentConfiguration().getConfig(AopConfig.class).isDebug();

        CLASS_FILE_DIR = new File(AgentContext.getInstance().getAgentDirectory()
                                  + separator
                                  + AgentContext.TMP_DIR
                                  + separator
                                  + AgentContext.getInstance().getAppInstance().getQualifiedAppName()
                                  + separator
                                  + "classes");

        // clean up directories before startup
        // this is convenient for debugging
        try {
            Files.walk(CLASS_FILE_DIR.toPath())
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        } catch (IOException ignored) {
        }

        try {
            if (!CLASS_FILE_DIR.exists()) {
                CLASS_FILE_DIR.mkdirs();
            }
        } catch (Exception e) {
            log.error("log error", e);
        }
    }

    private final Set<String> types;

    public AopDebugger(Set<String> types) {
        this.types = types;
    }

    public static void saveClassToFile(DynamicType dynamicType) {
        if (!IS_DEBUG_ENABLED) {
            return;
        }
        try {
            log.info("Saving [{}] to [{}]...", dynamicType.getTypeDescription().getTypeName(), CLASS_FILE_DIR);
            dynamicType.saveIn(CLASS_FILE_DIR);
        } catch (Throwable e) {
            log.warn("Failed to save class {} to file." + dynamicType.getTypeDescription().getActualName(), e);
        }
    }

    @Override
    public void onTransformation(TypeDescription typeDescription,
                                 ClassLoader classLoader,
                                 JavaModule javaModule,
                                 boolean loaded, DynamicType dynamicType) {
        if (IS_DEBUG_ENABLED && this.types.contains(typeDescription.getTypeName())) {
            log.info("{} Transformed", typeDescription.getTypeName());
            saveClassToFile(dynamicType);
        }
    }
}
