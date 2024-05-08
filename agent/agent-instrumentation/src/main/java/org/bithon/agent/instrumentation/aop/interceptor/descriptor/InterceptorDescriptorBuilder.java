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

package org.bithon.agent.instrumentation.aop.interceptor.descriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author frankchen
 * @date Jan 13, 2020 1:07:41 PM
 */
public class InterceptorDescriptorBuilder {

    private String targetClass;
    private boolean debug;
    private final List<MethodPointCutDescriptor> pointCuts = new ArrayList<>();

    public static InterceptorDescriptorBuilder forClass(String targetClass) {
        return new InterceptorDescriptorBuilder().targetClass(targetClass);
    }

    public MethodPointCutDescriptorBuilder hook() {
        return new MethodPointCutDescriptorBuilder(this);
    }

    public InterceptorDescriptor build() {
        if (debug) {
            for (MethodPointCutDescriptor pointCut : pointCuts) {
                pointCut.setDebug(debug);
            }
        }
        return new InterceptorDescriptor(debug, targetClass, pointCuts.toArray(new MethodPointCutDescriptor[0]));
    }

    public InterceptorDescriptorBuilder targetClass(String targetClass) {
        this.targetClass = targetClass;
        return this;
    }

    public InterceptorDescriptorBuilder debug() {
        this.debug = true;
        return this;
    }

    void add(MethodPointCutDescriptor methodPointCutDescriptor) {
        pointCuts.add(methodPointCutDescriptor);
    }
}
