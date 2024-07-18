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

package org.bithon.component.commons.expression.function.builtin.string;

import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.function.AbstractFunction;
import org.bithon.component.commons.expression.function.Parameter;

import java.util.Arrays;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/7/18 21:00
 */
public class EndsWith extends AbstractFunction {
    public EndsWith() {
        super("endsWith",
              Arrays.asList(new Parameter(IDataType.STRING), new Parameter(IDataType.STRING)),
              IDataType.BOOLEAN);
    }

    @Override
    public Object evaluate(List<Object> parameters) {
        String str = (String) parameters.get(0);
        String suffix = (String) parameters.get(1);
        return str != null && suffix != null && str.endsWith(suffix);
    }
}
