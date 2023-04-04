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

import org.bithon.shaded.net.bytebuddy.asm.AsmVisitorWrapper;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.description.type.TypeDescription;
import org.bithon.shaded.net.bytebuddy.implementation.Implementation;
import org.bithon.shaded.net.bytebuddy.jar.asm.MethodVisitor;
import org.bithon.shaded.net.bytebuddy.pool.TypePool;

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
public class InterceptorRecorder implements AsmVisitorWrapper.ForDeclaredMethods.MethodVisitorWrapper {

    public static final InterceptorRecorder INSTANCE = new InterceptorRecorder();

    public static class InstrumentedMethod {
        private final String returnType;
        private final String methodName;
        private final String parameters;

        public InstrumentedMethod(String returnType, String methodName, String parameters) {
            this.returnType = returnType;
            this.methodName = methodName;
            this.parameters = parameters;
        }

        public String getReturnType() {
            return returnType;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getParameters() {
            return parameters;
        }
    }

    private final Map<String, List<InstrumentedMethod>> instrumentedMethods = Collections.synchronizedMap(new HashMap<>());

    public Map<String, List<InstrumentedMethod>> getInstrumentedMethods() {
        return new LinkedHashMap<>(instrumentedMethods);
    }

    @Override
    public MethodVisitor wrap(TypeDescription instrumentedType,
                              MethodDescription instrumentedMethod,
                              MethodVisitor methodVisitor,
                              Implementation.Context implementationContext,
                              TypePool typePool,
                              int writerFlags,
                              int readerFlags) {

        StringBuilder parameters = new StringBuilder(32);
        for (Object parameterType : instrumentedMethod.getParameters()) {
            if (parameters.length() > 0) {
                parameters.append(',');
            }
            parameters.append(parameterType);
        }

        instrumentedMethods.computeIfAbsent(instrumentedType.getName(), v -> Collections.synchronizedList(new ArrayList<>()))
                           .add(new InstrumentedMethod(instrumentedMethod.getReturnType().getTypeName(),
                                                       instrumentedMethod.getName(),
                                                       parameters.toString()));

        return methodVisitor;
    }
}
