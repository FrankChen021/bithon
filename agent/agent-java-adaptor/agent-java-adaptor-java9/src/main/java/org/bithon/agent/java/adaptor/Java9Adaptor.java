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

package org.bithon.agent.java.adaptor;

import java.lang.instrument.Instrumentation;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * JDK 9+ specific adaptor implementation.
 * Handles module system configuration for Java 9 and later versions.
 */
public class Java9Adaptor implements IJavaAdaptor {

    public Java9Adaptor(Instrumentation inst) {
        // Open package to this class as this class contains internal methods to access
        openPackages(inst, Object.class, Map.of("jdk.internal.misc", Java9Adaptor.class));
    }

    @Override
    public void openPackages(Instrumentation inst,
                             Class<?> classFromSourceModule,
                             Map<String, Class<?>> openPackageTo) {

        Map<String, Set<Module>> openPackageToModules = new HashMap<>();
        for (Map.Entry<String, Class<?>> entry : openPackageTo.entrySet()) {
            String packageName = entry.getKey();
            Class<?> clazz = entry.getValue();
            openPackageToModules.put(packageName, Collections.singleton(clazz.getModule()));
        }

        inst.redefineModule(
            classFromSourceModule.getModule(),
            Collections.emptySet(), // extra reads
            Collections.emptyMap(), // extra exports
            openPackageToModules,
            Collections.emptySet(), // extra uses
            Collections.emptyMap()  // extra provides
        );
    }

    @Override
    public long getMaxDirectMemory() {
        return jdk.internal.misc.VM.maxDirectMemory();
    }
}
