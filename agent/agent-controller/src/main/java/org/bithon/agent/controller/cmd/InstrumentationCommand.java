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

package org.bithon.agent.controller.cmd;

import org.bithon.agent.instrumentation.aop.interceptor.InterceptorManager;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorSupplier;
import org.bithon.agent.instrumentation.aop.interceptor.installer.InstallerRecorder;
import org.bithon.agent.rpc.brpc.cmd.IInstrumentationCommand;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Frank Chen
 * @date 4/4/23 10:19 pm
 */
public class InstrumentationCommand implements IInstrumentationCommand, IAgentCommand {

    /**
     * Composite key for deduplication based on interceptor name, classLoader, class name,
     * return type, method name, parameters, and interceptor index
     */
    private static class DeduplicationKey {
        private final String interceptorName;
        private final String classLoader;
        private final String className;
        private final String returnType;
        private final String methodName;
        private final String parameters;
        private final int interceptorIndex;

        public DeduplicationKey(String interceptorName, String classLoader, String className,
                                String returnType, String methodName, String parameters, int interceptorIndex) {
            this.interceptorName = interceptorName;
            this.classLoader = classLoader;
            this.className = className;
            this.returnType = returnType;
            this.methodName = methodName;
            this.parameters = parameters;
            this.interceptorIndex = interceptorIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DeduplicationKey that = (DeduplicationKey) o;
            return interceptorIndex == that.interceptorIndex &&
                   Objects.equals(interceptorName, that.interceptorName) &&
                   Objects.equals(classLoader, that.classLoader) &&
                   Objects.equals(className, that.className) &&
                   Objects.equals(returnType, that.returnType) &&
                   Objects.equals(methodName, that.methodName) &&
                   Objects.equals(parameters, that.parameters);
        }

        @Override
        public int hashCode() {
            return Objects.hash(interceptorName, classLoader, className, returnType, methodName, parameters, interceptorIndex);
        }
    }

    @Override
    public List<InstrumentedMethod> getInstrumentedMethods() {
        Map<DeduplicationKey, InstrumentedMethod> deduplicationMap = new HashMap<>();

        for (InstallerRecorder.InstrumentedMethod method : InstallerRecorder.INSTANCE.getInstrumentedMethods()) {
            InterceptorSupplier supplier = InterceptorManager.INSTANCE.getSupplier(method.getInterceptorIndex());

            String interceptorName = method.getInterceptorName();
            String classLoader = supplier != null ? supplier.getClassLoaderId() : null;
            String className = method.getType();
            String returnType = method.getReturnType();
            String methodName = method.getMethodName();
            String parameters = method.getParameters();
            int interceptorIndex = method.getInterceptorIndex();

            // Check if we have duplicate records first
            // The InterceptorNameResolver will be called twice for AroundAdvice, one for the OnEnter, the other for onExit
            // We deduplicate here because the command is less frequently executed
            DeduplicationKey key = new DeduplicationKey(interceptorName,
                                                        classLoader,
                                                        className,
                                                        returnType,
                                                        methodName,
                                                        parameters,
                                                        interceptorIndex);
            InstrumentedMethod existing = deduplicationMap.get(key);
            if (existing != null) {
                // A duplicate record found, skip it
                continue;
            }

            // Create new entry
            InstrumentedMethod m = new InstrumentedMethod();
            m.interceptor = interceptorName;
            if (supplier != null) {
                m.clazzLoader = classLoader;
                m.hitCount = supplier.isInterceptorInstantiated() ? supplier.get().getHitCount() : 0;
                m.clazzName = className;
                m.returnType = returnType;
                m.methodName = methodName;
                m.isStatic = method.isStatic();
                m.parameters = parameters;
                m.instrumentException = supplier.getException();
                m.exceptionCount = supplier.isInterceptorInstantiated() ? supplier.get().getExceptionCount() : 0;
                m.lastExceptionTime = new Timestamp(supplier.isInterceptorInstantiated() ? supplier.get().getLastExceptionTime() : 0L);
                m.lastException = supplier.isInterceptorInstantiated() ? InterceptorSupplier.getStackTrace(supplier.get().getLastException()) : null;
                m.lastHitTime = new Timestamp(supplier.isInterceptorInstantiated() ? supplier.get().getLastHitTime() : 0L);
            } else {
                m.clazzLoader = classLoader;
                m.hitCount = 0;
                m.clazzName = className;
                m.returnType = returnType;
                m.methodName = methodName;
                m.isStatic = method.isStatic();
                m.parameters = parameters;
            }
            deduplicationMap.put(key, m);
        }

        return new ArrayList<>(deduplicationMap.values());
    }
}
