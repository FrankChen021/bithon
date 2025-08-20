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
import java.util.List;

/**
 * @author Frank Chen
 * @date 4/4/23 10:19 pm
 */
public class InstrumentationCommand implements IInstrumentationCommand, IAgentCommand {
    @Override
    public List<InstrumentedMethod> getInstrumentedMethods() {
        List<InstrumentedMethod> returning = new ArrayList<>();

        for (InstallerRecorder.InstrumentedMethod method : InstallerRecorder.INSTANCE.getInstrumentedMethods()) {
            InterceptorSupplier supplier = InterceptorManager.INSTANCE.getSupplier(method.getInterceptorIndex());

            InstrumentedMethod m = new InstrumentedMethod();
            m.interceptor = method.getInterceptorName();
            if (supplier != null) {
                m.clazzLoader = supplier.getClassLoaderId();
                m.hitCount = supplier.isInterceptorInstantiated() ? supplier.get().getHitCount() : 0;
                m.clazzName = method.getType();
                m.returnType = method.getReturnType();
                m.methodName = method.getMethodName();
                m.isStatic = method.isStatic();
                m.parameters = method.getParameters();
                m.instrumentException = supplier.getException();
                m.exceptionCount = supplier.isInterceptorInstantiated() ? supplier.get().getExceptionCount() : 0;
                m.lastExceptionTime = new Timestamp(supplier.isInterceptorInstantiated() ? supplier.get().getLastExceptionTime() : 0L);
                m.lastException = supplier.isInterceptorInstantiated() ? InterceptorSupplier.getStackTrace(supplier.get().getLastException()) : null;
            } else {
                m.clazzLoader = null;
                m.hitCount = 0;
                m.clazzName = method.getType();
                m.returnType = method.getReturnType();
                m.methodName = method.getMethodName();
                m.isStatic = method.isStatic();
                m.parameters = method.getParameters();
            }
            returning.add(m);
        }
        return returning;
    }
}
