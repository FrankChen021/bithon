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

package org.bithon.agent.bootstrap.aop.advice;

import shaded.net.bytebuddy.asm.Advice;
import shaded.net.bytebuddy.description.method.MethodDescription;
import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.implementation.bytecode.assign.Assigner;
import shaded.net.bytebuddy.implementation.bytecode.constant.MethodConstant;

/**
 * See <a href="https://github.com/raphw/byte-buddy/issues/1210">this issue</a> on github for more details
 *
 * @author Frank Chen
 * @date 22/2/22 8:30 PM
 */
public class TargetMethodResolver implements Advice.OffsetMapping {
    @Override
    public Target resolve(TypeDescription instrumentedType,
                          MethodDescription instrumentedMethod,
                          Assigner assigner,
                          Advice.ArgumentHandler argumentHandler,
                          Sort sort) {
        return new Target.ForStackManipulation(MethodConstant.of(instrumentedMethod.asDefined()).cached());
    }
}
