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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Frank Chen
 * @date 10/3/23 8:54 pm
 */
public class InstallerRecorder {

    public static final InstallerRecorder INSTANCE = new InstallerRecorder();

    public static class InstrumentedMethod {
        private final String interceptor;
        private final String type;
        private final String returnType;
        private final String methodName;
        private final boolean isStatic;
        private final String parameters;


        public InstrumentedMethod(String interceptor, String type, String returnType, String methodName, boolean aStatic, String parameters) {
            this.interceptor = interceptor;
            this.returnType = returnType;
            this.methodName = methodName;
            this.isStatic = aStatic;
            this.parameters = parameters;
            this.type = type;
        }

        public String getInterceptor() {
            return interceptor;
        }

        public String getType() {
            return type;
        }

        public String getReturnType() {
            return returnType;
        }

        public String getMethodName() {
            return methodName;
        }

        public boolean isStatic() {
            return isStatic;
        }

        public String getParameters() {
            return parameters;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            InstrumentedMethod that = (InstrumentedMethod) o;
            return isStatic == that.isStatic
                    && Objects.equals(interceptor, that.interceptor)
                    && Objects.equals(type, that.type)
                    && Objects.equals(returnType, that.returnType)
                    && Objects.equals(methodName, that.methodName)
                    && Objects.equals(parameters, that.parameters);
        }

        @Override
        public int hashCode() {
            return Objects.hash(interceptor, type, returnType, methodName, isStatic, parameters);
        }
    }

    /**
     * key - interceptor name
     * val - instrumented method
     */
    private final Set<InstrumentedMethod> instrumentedMethods = Collections.synchronizedSet(new HashSet<>());

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
    public void addInterceptedMethod(String interceptor, TypeDescription instrumentedType, MethodDescription instrumentedMethod) {
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

        InstrumentedMethod method = new InstrumentedMethod(interceptor,
                                                           instrumentedType.getName(),
                                                           instrumentedMethod.getReturnType().getTypeName(),
                                                           instrumentedMethod.isConstructor() ? "<ctor>" : instrumentedMethod.getName(),
                                                           instrumentedMethod.isStatic(),
                                                           parameters.toString());
        this.instrumentedMethods.add(method);
    }
}
