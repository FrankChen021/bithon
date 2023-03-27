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

package org.bithon.agent.instrumentation.aop.interceptor.expression.matcher;

import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.description.method.ParameterList;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatcher;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/26 21:28
 */
public interface IArgumentMatcher extends ElementMatcher<MethodDescription> {

    void setPropertyAccessor(IPropertyAccessor accessor);

    void setExpected(Object expected);

    interface IPropertyAccessor {
        Object get(ParameterList<?> parameterList);
    }

    class EQ implements IArgumentMatcher {
        private IPropertyAccessor propertyAccessor;
        private Object expected;

        @Override
        public void setPropertyAccessor(IPropertyAccessor accessor) {
            this.propertyAccessor = accessor;
        }

        @Override
        public void setExpected(Object expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(MethodDescription target) {
            Object lValue = propertyAccessor.get(target.getParameters());
            if (lValue == null) {
                return expected == null;
            }
            return lValue.equals(expected);
        }
    }


}
