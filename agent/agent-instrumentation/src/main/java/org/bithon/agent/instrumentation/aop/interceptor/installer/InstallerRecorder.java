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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 10/3/23 8:54 pm
 */
public class InstallerRecorder {

    public static final InstallerRecorder INSTANCE = new InstallerRecorder();

    public static class InstrumentedMethod {
        private final String returnType;
        private final String methodName;
        private final boolean isStatic;
        private final String parameters;
        private final String interceptor;

        public InstrumentedMethod(String returnType,
                                  String methodName,
                                  boolean aStatic,
                                  String parameters,
                                  String interceptor) {
            this.returnType = returnType;
            this.methodName = methodName;
            this.isStatic = aStatic;
            this.parameters = parameters;
            this.interceptor = interceptor;
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

        public String getInterceptor() {
            return interceptor;
        }
    }

    private final Map<String, List<InstrumentedMethod>> instrumentedMethods = Collections.synchronizedMap(new HashMap<>());

    public Map<String, List<InstrumentedMethod>> getInstrumentedMethods() {
        return new LinkedHashMap<>(instrumentedMethods);
    }

    public InstallerRecorder() {
    }

    public void addInterceptedMethod(String interceptor,
                                     TypeDescription instrumentedType,
                                     MethodDescription instrumentedMethod) {
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

        instrumentedMethods.computeIfAbsent(instrumentedType.getName(), v -> Collections.synchronizedList(new ArrayList<>()))
                           .add(new InstrumentedMethod(instrumentedMethod.getReturnType().getTypeName(),
                                                       instrumentedMethod.isConstructor() ? "<ctor>" : instrumentedMethod.getName(),
                                                       instrumentedMethod.isStatic(),
                                                       parameters.toString(),
                                                       interceptor));
    }
}