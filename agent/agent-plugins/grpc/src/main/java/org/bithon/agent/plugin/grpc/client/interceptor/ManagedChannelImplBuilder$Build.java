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

package org.bithon.agent.plugin.grpc.client.interceptor;

import io.grpc.internal.ManagedChannelImplBuilder;
import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.bithon.agent.instrumentation.bytecode.ClassCopier;
import org.bithon.agent.plugin.grpc.ShadedGrpcList;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link ManagedChannelImplBuilder#build()}
 *
 * @author Frank Chen
 * @date 22/12/22 9:48 pm
 */
public class ManagedChannelImplBuilder$Build extends BeforeInterceptor {

    private final Map<String, String> shadedGrpcClassMap = new HashMap<>();
    private final List<String> shadedGrpcList = ConfigurationManager.getInstance().getConfig(ShadedGrpcList.class);

    @Override
    public void before(AopContext aopContext) {
        String targetClazzName = aopContext.getTargetClass().getName();
        if (shadedGrpcList.isEmpty() || targetClazzName.startsWith("io.grpc.")) {
            // No shaded gRPC or current target is not a shaded one, then create a default interceptor
            // Note: use string name instead of class name because this class might be executed in a shaded grpc lib
            createInterceptor(aopContext, "org.bithon.agent.plugin.grpc.client.interceptor.ClientCallInterceptor");
            return;
        }

        String interceptor = shadedGrpcClassMap.get(targetClazzName);
        if (interceptor != null) {
            // Know the interceptor for this target, then create it by its name
            createInterceptor(aopContext, interceptor);
            return;
        }

        // Current target is a shaded gRPC, we need to create a ClientCallInterceptor class for this shaded gRPC
        synchronized (shadedGrpcClassMap) {
            // double check
            if (!shadedGrpcClassMap.containsKey(targetClazzName)) {

                //
                // First, find the shaded package name by current target class name
                //
                // io.grpc.internal.ManagedChannelImplBuilder
                // ab.cd.ef.internal.ManagedChannelImplBuilder
                String[] parts = targetClazzName.split("\\.");
                int i = parts.length - 1 - 1;
                for (; i >= 0; i--) {
                    if ("internal".equals(parts[i])) {
                        break;
                    }
                }
                StringBuilder shadedPackage = new StringBuilder(32);
                for (int j = 0; j < i; j++) {
                    if (shadedPackage.length() > 0) {
                        shadedPackage.append('.');
                    }
                    shadedPackage.append(parts[j]);
                }

                // Create shaded ClientCallInterceptor
                String currentPackage = this.getClass().getPackage().getName();
                String clientInterceptor = currentPackage + "." + shadedPackage + ".ShadedClientCallInterceptor";
                try {
                    new ClassCopier()
                        .changePackage("io.grpc", shadedPackage.toString())
                        .copyClass(currentPackage + ".ClientCallInterceptor$TracedClientCallListener",
                                   currentPackage + "." + shadedPackage + ".ShadedTracedClientCallListener")
                        .copyClass(currentPackage + ".ClientCallInterceptor$TracedClientCall", currentPackage + "." + shadedPackage + ".ShadedTracedClientCall")
                        .copyClass(currentPackage + ".ClientCallInterceptor", clientInterceptor)
                        .to(this.getClass().getClassLoader());

                    // Save for future use
                    shadedGrpcClassMap.put(targetClazzName, clientInterceptor);
                } catch (IOException e) {
                    LoggerFactory.getLogger(ManagedChannelImplBuilder$Build.class)
                                 .error("Error when creating class [{}], error: {}", clientInterceptor, e.getMessage());
                }
            }
        }

        createInterceptor(aopContext, shadedGrpcClassMap.get(targetClazzName));
    }

    private void createInterceptor(AopContext aopContext, String clientInterceptor) {
        try {
            Object builder = aopContext.getTarget();

            // Create client call interceptor object
            Class<?> clazz = Class.forName(clientInterceptor, true, this.getClass().getClassLoader());
            Object interceptor = clazz.getConstructor(String.class).newInstance((String) ReflectionUtils.getFieldValue(builder, "target"));

            // Invoke intercept method on builder
            Method interceptMethod = builder.getClass().getDeclaredMethod("intercept", List.class);
            interceptMethod.invoke(builder, Collections.singletonList(interceptor));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }
}
