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
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.context.AgentContext;
import org.bithon.agent.core.shading.ClassShader;
import org.bithon.agent.plugin.grpc.ShadedGrpcList;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link ManagedChannelImplBuilder#build()}
 *
 * @author Frank Chen
 * @date 22/12/22 9:48 pm
 */
public class ManagedChannelImplBuilder$Build extends AbstractInterceptor {

    private final Set<String> shadedGrpc = new HashSet<>();
    private final List<String> shadedGrpcList = AgentContext.getInstance().getAgentConfiguration().getConfig(ShadedGrpcList.class);

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {
        String clientInterceptor = "org.bithon.agent.plugin.grpc.client.interceptor.ClientCallInterceptor";

        if (!shadedGrpcList.isEmpty()) {

            String targetClazzName = aopContext.getTargetClass().getName();
            if (!targetClazzName.startsWith("io.grpc")) {

                // Current target is a shaded gRPC, we need to create a ClientCallInterceptor for this shaded gRPC
                synchronized (shadedGrpc) {
                    if (shadedGrpc.add(targetClazzName)) {
                        // io.grpc.internal.ManagedChannelImplBuilder
                        // ab.cd.ef.internal.ManagedChannelImplBuilder
                        String[] parts = targetClazzName.split("\\.");
                        int i = parts.length - 1 - 1;
                        for (; i >= 0; i--) {
                            if ("internal".equals(parts[i])) {
                                break;
                            }
                        }
                        StringBuilder newPackageName = new StringBuilder(32);
                        for (int j = 0; j < i; j++) {
                            if (newPackageName.length() > 0) {
                                newPackageName.append('.');
                            }
                            newPackageName.append(parts[j]);
                        }

                        // Create shaded ClientCallInterceptor
                        ClassShader shader = new ClassShader("io.grpc", newPackageName.toString());
                        clientInterceptor = newPackageName + ".ClientCallInterceptor";
                        try {
                            shader.shade(ClientCallInterceptor.class, clientInterceptor, this.getClass().getClassLoader());
                        } catch (IOException e) {
                            LoggerFactory.getLogger(ManagedChannelImplBuilder$Build.class)
                                         .error("Error when creating class [{}], error: {}", clientInterceptor, e.getMessage());
                        }
                    }
                }
            }
        }


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

        return InterceptionDecision.SKIP_LEAVE;
    }
}
