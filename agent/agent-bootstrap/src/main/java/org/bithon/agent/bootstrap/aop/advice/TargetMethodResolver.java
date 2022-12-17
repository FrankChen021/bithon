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

import org.bithon.shaded.net.bytebuddy.asm.Advice;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.description.type.TypeDescription;
import org.bithon.shaded.net.bytebuddy.implementation.bytecode.assign.Assigner;
import org.bithon.shaded.net.bytebuddy.implementation.bytecode.constant.MethodConstant;

import javax.annotation.Nonnull;

/**
 * See <a href="https://github.com/raphw/byte-buddy/issues/1210">this issue</a> on GitHub for more details
 *
 * @author frank.chen021@outlook.com
 * @date 22/2/22 8:30 PM
 */
public class TargetMethodResolver implements Advice.OffsetMapping {
    @Nonnull
    @Override
    public Target resolve(@Nonnull TypeDescription instrumentedType,
                          MethodDescription instrumentedMethod,
                          @Nonnull Assigner assigner,
                          @Nonnull Advice.ArgumentHandler argumentHandler,
                          @Nonnull Sort sort) {
        return new Target.ForStackManipulation(MethodConstant.of(instrumentedMethod.asDefined()).cached());
    }
}
