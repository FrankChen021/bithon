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

package org.bithon.agent.instrumentation.aop.interceptor.precondition;


import org.bithon.shaded.net.bytebuddy.description.type.TypeDescription;

import java.lang.management.ManagementFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 14/4/25 8:30 pm
 */
public class JdkVersionPrecondition {

    private static final int MAJOR_VERSION;

    static {
        String specVersion = ManagementFactory.getRuntimeMXBean().getSpecVersion();
        String[] versionParts = specVersion.split("\\.");
        if (versionParts[0].equals("1")) {
            // For Java 1.x (e.g., 1.8)
            MAJOR_VERSION = Integer.parseInt(versionParts[1]);
        } else {
            // For Java 9 and above
            MAJOR_VERSION = Integer.parseInt(versionParts[0]);
        }
    }


    /**
     * @param min inclusive
     * @param max inclusive
     */
    public static IInterceptorPrecondition between(int min, int max) {
        return new IInterceptorPrecondition() {

            @Override
            public boolean matches(ClassLoader classLoader, TypeDescription typeDescription) {
                return MAJOR_VERSION >= min && MAJOR_VERSION <= max;
            }

            @Override
            public String toString() {
                return "Jdk in [" + min + ", " + max + "]";
            }
        };
    }

    public static IInterceptorPrecondition gt(int version) {
        return new IInterceptorPrecondition() {

            @Override
            public boolean matches(ClassLoader classLoader, TypeDescription typeDescription) {
                return MAJOR_VERSION > version;
            }

            @Override
            public String toString() {
                return "Jdk > " + version;
            }
        };
    }

    public static IInterceptorPrecondition gte(int version) {
        return new IInterceptorPrecondition() {
            @Override
            public boolean matches(ClassLoader classLoader, TypeDescription typeDescription) {
                return MAJOR_VERSION >= version;
            }

            @Override
            public String toString() {
                return "Jdk >= " + version;
            }
        };
    }

    public static IInterceptorPrecondition eq(int version) {
        return new IInterceptorPrecondition() {
            @Override
            public boolean matches(ClassLoader classLoader, TypeDescription typeDescription) {
                return MAJOR_VERSION == version;
            }

            @Override
            public String toString() {
                return "Jdk == " + version;
            }
        };
    }
}
