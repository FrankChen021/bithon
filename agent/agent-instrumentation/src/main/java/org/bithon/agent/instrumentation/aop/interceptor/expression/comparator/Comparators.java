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

package org.bithon.agent.instrumentation.aop.interceptor.expression.comparator;

import org.bithon.agent.instrumentation.aop.interceptor.expression.IValueSupplier;
import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/26 21:28
 */
public interface Comparators {

    boolean matches(MethodDescription description);

    class EQ implements Comparators {
        private final IValueSupplier left;
        private final IValueSupplier right;

        public EQ(IValueSupplier left, IValueSupplier right) {
            this.left = left;
            this.right = right;
        }

        @Override
        public boolean matches(MethodDescription description) {
            Object lValue = left.get(description);
            Object rValue = right.get(description);
            return lValue.equals(rValue);
        }
    }
}
