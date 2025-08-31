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
    
    @Override
    public void openModules(Instrumentation inst) {
        try {
            // Get the java.base module from a core JDK class
            Module javaBase = Object.class.getModule();
            // Get the module that contains this class
            Module thisModule = JavaAdaptorFactory.class.getModule();

            // Prepare exports and opens maps: package name -> set of target modules
            Map<String, Set<Module>> extraExports = new HashMap<>();
            Map<String, Set<Module>> extraOpens = new HashMap<>();

            // Export sun.net.www to this module
            extraExports.put("sun.net.www", Collections.singleton(thisModule));

            // Open and export jdk.internal.misc so agent can access internal VM APIs
            extraOpens.put("jdk.internal.misc", Collections.singleton(thisModule));
            extraExports.put("jdk.internal.misc", Collections.singleton(thisModule));

            // Use Instrumentation.redefineModule directly
            inst.redefineModule(
                javaBase,
                Collections.emptySet(), // extra reads
                extraExports,
                extraOpens,
                Collections.emptySet(), // extra uses
                Collections.emptyMap()  // extra provides
            );

            System.err.println("Jdk9Adaptor: successfully redefined java.base exports/opens for agent module: " + thisModule);
        } catch (Throwable t) {
            // Resist failing the JVM startup; just log the problem so developer can see it.
            System.err.println("Jdk9Adaptor: failed to redefine module exports/opens: " + t);
        }
    }
}
