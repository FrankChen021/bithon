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

import org.bithon.component.commons.expression.function.IDataType;
import org.bithon.component.commons.expression.function.IFunction;
import org.bithon.component.commons.expression.function.Parameter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Manage the definitions of all the supported functions.
 * It's a safe list so that we can ensure user input is safe and any injection can be rejected.
 *
 * @author frank.chen021@outlook.com
 * @date 2022/11/2 17:34
 */
public class Functions implements IFunctionProvider {
    private static final Functions INSTANCE = new Functions();

    public static Functions getInstance() {
        return INSTANCE;
    }

    private final Map<String, IFunction> functionMap = new HashMap<>(17);

    /**
     * <p>
     * <a href="http://h2database.com/html/functions.html">H2 Functions</a>
     * <p>
     * <a href="https://clickhouse.com/docs/en/sql-reference/functions/string-functions">ClickHouse Functions</a>
     */
    public Functions() {
        register(new AbstractFunction("round", Arrays.asList(new Parameter(IDataType.DOUBLE), new Parameter(IDataType.LONG))) {
            @Override
            public Object evaluate(List<Object> parameters) {
                double i0 = ((Number) parameters.get(0)).doubleValue();
                int scale = ((Number) parameters.get(1)).intValue();
                return BigDecimal.valueOf(i0).setScale(scale, RoundingMode.HALF_UP);
            }
        });

        // CK Only
        register(new AbstractFunction("startsWith", Arrays.asList(new Parameter(IDataType.STRING), new Parameter(IDataType.STRING))) {
            @Override
            public Object evaluate(List<Object> parameters) {
                String str = (String) parameters.get(0);
                String prefix = (String) parameters.get(1);
                return str != null && prefix != null && str.startsWith(prefix);
            }
        });

        register(new AbstractFunction("endsWith", Arrays.asList(new Parameter(IDataType.STRING), new Parameter(IDataType.STRING))) {
            @Override
            public Object evaluate(List<Object> parameters) {
                String str = (String) parameters.get(0);
                String suffix = (String) parameters.get(1);
                return str != null && suffix != null && str.endsWith(suffix);
            }
        });

        register(new AbstractFunction("hasToken", Arrays.asList(new Parameter(IDataType.STRING), new Parameter(IDataType.STRING))) {
            @Override
            public Object evaluate(List<Object> parameters) {
                String str = (String) parameters.get(0);
                String token = (String) parameters.get(1);
                return str != null && token != null && str.contains(token);
            }
        });

        register(new AbstractFunction("lower", Collections.singletonList(new Parameter(IDataType.STRING))) {
            @Override
            public Object evaluate(List<Object> parameters) {
                String str = (String) parameters.get(0);
                return str != null ? str.toLowerCase(Locale.ENGLISH) : null;
            }
        });

        register(new AbstractFunction("upper", Collections.singletonList(new Parameter(IDataType.STRING))) {
            @Override
            public Object evaluate(List<Object> parameters) {
                String str = (String) parameters.get(0);
                return str != null ? str.toUpperCase(Locale.ENGLISH) : null;
            }
        });

        register(new AbstractFunction("substring", Arrays.asList(new Parameter(IDataType.STRING),
                                                                 new Parameter(IDataType.LONG),
                                                                 new Parameter(IDataType.LONG))) {

            @Override
            public Object evaluate(List<Object> parameters) {
                String str = (String) parameters.get(0);
                Number offset = (Number) parameters.get(1);
                Number length = (Number) parameters.get(2);
                return str == null ? null : str.substring(offset.intValue(), offset.intValue() + length.intValue());
            }
        });

        register(new AbstractFunction("trim", Collections.singletonList(new Parameter(IDataType.STRING))) {
            @Override
            public Object evaluate(List<Object> parameters) {
                String str = (String) parameters.get(0);
                return str == null ? null : str.trim();
            }
        });

        register(new AbstractFunction("trimLeft", Collections.singletonList(new Parameter(IDataType.STRING))) {
            @Override
            public Object evaluate(List<Object> parameters) {
                String str = (String) parameters.get(0);
                if (str == null) {
                    return null;
                }

                int index = 0;

                //noinspection StatementWithEmptyBody
                for (int size = str.length(); index < size && Character.isWhitespace(str.charAt(index)); index++) {
                }

                return index == 0 ? str : str.substring(index);
            }
        });

        register(new AbstractFunction("trimRight", Collections.singletonList(new Parameter(IDataType.STRING))) {
            @Override
            public Object evaluate(List<Object> parameters) {
                String str = (String) parameters.get(0);
                if (str == null) {
                    return null;
                }

                int index = str.length() - 1;

                //noinspection StatementWithEmptyBody
                for (; index >= 0 && Character.isWhitespace(str.charAt(index)); index--) {
                }

                return index < 0 ? "" : str.substring(0, index + 1);
            }
        });

        // CK Only
        register(new AbstractFunction("length", Collections.singletonList(new Parameter(IDataType.STRING))) {
            @Override
            public Object evaluate(List<Object> parameters) {
                String str = (String) parameters.get(0);
                return str == null ? 0 : str.length();
            }
        });

        register(new AbstractFunction("toStartOfMinute", new Parameter(IDataType.LONG)) {
            @Override
            public Object evaluate(List<Object> parameters) {
                Object o = parameters.get(0);
                return (o instanceof Number) ? ((Number) o).longValue() / 1000 / 60 : 0;
            }
        });
    }

    private void register(IFunction function) {
        functionMap.put(function.getName().toLowerCase(Locale.ENGLISH), function);
    }

    @Override
    public IFunction getFunction(String name) {
        return functionMap.get(name.toLowerCase(Locale.ENGLISH));
    }
}
