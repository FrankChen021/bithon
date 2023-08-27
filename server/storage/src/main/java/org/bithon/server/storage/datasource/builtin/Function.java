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

package org.bithon.server.storage.datasource.builtin;

import lombok.Getter;
import org.bithon.component.commons.expression.function.IFunction;
import org.bithon.component.commons.expression.function.Parameter;

import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/2 17:31
 */
@Getter
public class Function implements IFunction {
    private final String name;
    private final List<Parameter> parameters;

    public Function(String name) {
        this(name, Collections.emptyList());
    }

    public Function(String name, List<Parameter> parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    @Override
    public void validateParameter(int index, Object parameter) {

    }

    @Override
    public Object evaluate(List<Object> parameters) {
        return null;
    }
}
