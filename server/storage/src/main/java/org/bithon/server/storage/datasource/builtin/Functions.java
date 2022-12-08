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

import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.datasource.ast.FieldExpressionParser;
import org.bithon.server.storage.datasource.typing.DoubleValueType;
import org.bithon.server.storage.datasource.typing.LongValueType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manage the defs of all the supported functions
 *
 * @author frank.chen021@outlook.com
 * @date 2022/11/2 17:34
 */
public class Functions {
    private static final Functions INSTANCE = new Functions();

    public static Functions getInstance() {
        return INSTANCE;
    }

    private final Map<String, Function> functionMap = new HashMap<>(17);

    public Functions() {
        register(new Function("round",
                              Arrays.asList(new Parameter(DoubleValueType.INSTANCE), new Parameter(LongValueType.INSTANCE)),
                              (index, expression) -> {
                                  if (index == 1 && expression.getToken(FieldExpressionParser.NUMBER, 0) == null) {
                                      throw new RuntimeException(StringUtils.format(
                                          "Function [round] requires the 2nd parameter as a constant value, but given an expression as %s",
                                          expression.getText()));
                                  }
                              }
        ));
    }

    private void register(Function function) {
        functionMap.put(function.getName().toLowerCase(Locale.ENGLISH), function);
    }

    public Function getFunction(String name) {
        return functionMap.get(name.toLowerCase(Locale.ENGLISH));
    }
}
