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

package org.bithon.agent.instrumentation.aop.interceptor.descriptor;

/**
 * Defines which class should be transformed into IBithonObject subclasses
 *
 * @author frank.chen021@outlook.com
 * @date 2021/3/27 19:59
 */
public class BithonClassDescriptor {
    private final String[] targetClasses;
    private final boolean debug;

    private BithonClassDescriptor(String[] classes, boolean debug) {
        this.targetClasses = classes;
        this.debug = debug;
    }

    public static BithonClassDescriptor of(String... clazz) {
        return new BithonClassDescriptor(clazz, false);
    }

    public static BithonClassDescriptor of(String clazz, boolean debug) {
        return new BithonClassDescriptor(new String[]{clazz}, debug);
    }

    public String[] getTargetClasses() {
        return targetClasses;
    }

    public boolean isDebug() {
        return debug;
    }
}
