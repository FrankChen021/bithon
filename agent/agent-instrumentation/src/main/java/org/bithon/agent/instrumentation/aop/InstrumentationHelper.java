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

package org.bithon.agent.instrumentation.aop;

import org.bithon.agent.instrumentation.aop.debug.Debugger;
import org.bithon.shaded.net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/10 13:40
 */
public class InstrumentationHelper {
    private static Instrumentation inst;
    private static Debugger debugger;
    private static AgentBuilder.Listener errorHandler;

    public static Instrumentation getInstance() {
        return inst;
    }

    public static void setInstance(Instrumentation inst) {
        InstrumentationHelper.inst = inst;
    }

    public static void setAopDebugger(Debugger debugger) {
        InstrumentationHelper.debugger = debugger;
    }

    public static Debugger getAopDebugger() {
        return debugger;
    }

    public static void setErrorHandler(AgentBuilder.Listener errorHandler) {
        InstrumentationHelper.errorHandler = errorHandler;
    }

    public static AgentBuilder.Listener getErrorHandler() {
        return errorHandler;
    }
}
