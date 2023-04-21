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

package org.bithon.agent.rpc.brpc.cmd;

import org.bithon.component.brpc.BrpcService;
import org.bithon.component.brpc.message.serializer.Serializer;

import java.util.List;

/**
 * @author Frank Chen
 * @date 4/4/23 10:17 pm
 */
@BrpcService(name = "agent.instrumentation", serializer = Serializer.JSON_SMILE)
public interface IInstrumentationCommand {

    class InstrumentedMethod {
        public String interceptor;
        public String classLoader;
        public long hitCount;
        public String clazzName;
        public String returnType;
        public String methodName;
        public boolean isStatic;
        public String parameters;

        /**
         * Return the object in an object array.
         * The sequence of the values in the array MUST be in accordance with the sequence of fields
         */
        public Object[] toObjects() {
            return new Object[]{interceptor, classLoader, hitCount, clazzName, returnType, methodName, isStatic, parameters};
        }
    }

    List<InstrumentedMethod> getInstrumentedMethods();
}
