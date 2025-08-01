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

package org.bithon.agent.instrumentation.aop.interceptor.installer;

import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.description.method.ParameterDescription;
import org.bithon.shaded.net.bytebuddy.description.type.TypeDescription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Frank Chen
 * @date 10/3/23 8:54 pm
 */
public class InstallerRecorder {

    public static final InstallerRecorder INSTANCE = new InstallerRecorder();

    public static class InstrumentedMethod {
        private final int interceptorIndex;
        private final String interceptor;
        private final String type;
        private final MethodDescription instrumentedMethod;

        public InstrumentedMethod(int interceptorIndex,
                                  String interceptorClassName,
                                  TypeDescription instrumentedType,
                                  MethodDescription instrumentedMethod) {
            this.interceptorIndex = interceptorIndex;
            this.interceptor = interceptorClassName;

            this.type = instrumentedType.getTypeName();
            this.instrumentedMethod = instrumentedMethod;
        }

        public int getInterceptorIndex() {
            return this.interceptorIndex;
        }

        public String getInterceptorName() {
            return this.interceptor;
        }

        public String getType() {
            return type;
        }

        public String getReturnType() {
            return instrumentedMethod.getReturnType().getTypeName();
        }

        public String getMethodName() {
            return instrumentedMethod.isConstructor() ? "<ctor>" : instrumentedMethod.getName();
        }

        public boolean isStatic() {
            return instrumentedMethod.isStatic();
        }

        public String getParameters() {
            StringBuilder parameters = new StringBuilder(32);
            for (ParameterDescription parameter : instrumentedMethod.getParameters()) {
                if (parameters.length() > 0) {
                    parameters.append(", ");
                }

                String str = parameter.toString();
                if (parameter.getType().isArray()) {
                    str = str.substring(2);
                    str = str.replace(";", "[]");
                }
                parameters.append(str);
            }
            return parameters.toString();
        }
    }

    /**
     * key - interceptor name
     * val - instrumented method
     */
    private final List<InstrumentedMethod> instrumentedMethods = Collections.synchronizedList(new LinkedList<>());

    /**
     * A snapshot of instrumented methods
     */
    public List<InstrumentedMethod> getInstrumentedMethods() {
        return new ArrayList<>(instrumentedMethods);
    }

    /**
     * An interceptor can be applied to the same class which are loaded into two different class loaders.
     * For the given parameters of this method, in above case, actually they're the same
     */
    public void addInterceptedMethod(int interceptorIndex,
                                     String interceptorClassName,
                                     TypeDescription instrumentedType,
                                     MethodDescription instrumentedMethod) {
        InstrumentedMethod method = new InstrumentedMethod(interceptorIndex, interceptorClassName, instrumentedType, instrumentedMethod);
        this.instrumentedMethods.add(method);
    }

    public void deleteInterceptorIf(Predicate<InstrumentedMethod> predicate) {
        this.instrumentedMethods.removeIf(predicate);
    }
}
