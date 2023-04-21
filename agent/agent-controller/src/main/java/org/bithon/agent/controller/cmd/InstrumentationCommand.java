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
import org.bithon.agent.instrumentation.aop.interceptor.installer.InstallerRecorder;
import org.bithon.agent.rpc.brpc.cmd.IInstrumentationCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 4/4/23 10:19 pm
 */
public class InstrumentationCommand implements IInstrumentationCommand, IAgentCommand {
    @Override
    public List<InstrumentedMethod> getInstrumentedMethods() {
        List<InstrumentedMethod> returning = new ArrayList<>();

        InstallerRecorder.INSTANCE.getInstrumentedMethods()
                                  .forEach((type, methods) -> {
                                      for (InstallerRecorder.InstrumentedMethod method : methods) {

                                          Map<String, InterceptorManager.InterceptorSupplier> interceptorSuppliers = InterceptorManager.INSTANCE.getSuppliers(method.getInterceptor());
                                          if (interceptorSuppliers != null) {
                                              for (Map.Entry<String, InterceptorManager.InterceptorSupplier> entry : interceptorSuppliers.entrySet()) {
                                                  String clazzLoaderId = entry.getKey();
                                                  InterceptorManager.InterceptorSupplier supplier = entry.getValue();

                                                  InstrumentedMethod m = new InstrumentedMethod();
                                                  m.interceptor = (method.getInterceptor());
                                                  m.clazzLoader = clazzLoaderId;
                                                  m.hitCount = supplier.isInitialized() ? supplier.get().getHitCount() : 0;
                                                  m.clazzName = (type);
                                                  m.returnType = (method.getReturnType());
                                                  m.methodName = (method.getMethodName());
                                                  m.isStatic = (method.isStatic());
                                                  m.parameters = (method.getParameters());

                                                  returning.add(m);
                                              }
                                          } else {
                                              InstrumentedMethod m = new InstrumentedMethod();
                                              m.interceptor = (method.getInterceptor());
                                              m.clazzName = (type);
                                              m.returnType = (method.getReturnType());
                                              m.methodName = (method.getMethodName());
                                              m.isStatic = (method.isStatic());
                                              m.parameters = (method.getParameters());
                                              returning.add(m);
                                          }
                                      }
                                  });
        return returning;
    }
}
