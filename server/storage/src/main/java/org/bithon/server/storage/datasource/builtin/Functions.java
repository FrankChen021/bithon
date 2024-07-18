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

import org.bithon.component.commons.expression.function.IFunction;
import org.bithon.component.commons.expression.function.builtin.string.Concat;
import org.bithon.component.commons.expression.function.builtin.string.EndsWith;
import org.bithon.component.commons.expression.function.builtin.string.HasToken;
import org.bithon.component.commons.expression.function.builtin.string.Length;
import org.bithon.component.commons.expression.function.builtin.string.Lower;
import org.bithon.component.commons.expression.function.builtin.number.Round;
import org.bithon.component.commons.expression.function.builtin.string.StartsWith;
import org.bithon.component.commons.expression.function.builtin.string.Substring;
import org.bithon.component.commons.expression.function.builtin.string.Trim;
import org.bithon.component.commons.expression.function.builtin.string.TrimLeft;
import org.bithon.component.commons.expression.function.builtin.string.TrimRight;
import org.bithon.component.commons.expression.function.builtin.string.Upper;
import org.bithon.component.commons.expression.function.builtin.time.toStartOfMinute;

import java.util.HashMap;
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
        register(new Round());

        register(new StartsWith());
        register(new EndsWith());
        register(new HasToken());
        register(new Lower());
        register(new Upper());
        register(new Substring());
        register(new Trim());
        register(new TrimLeft());
        register(new TrimRight());
        register(new Length());
        register(new Concat());

        register(new toStartOfMinute());
    }

    private void register(IFunction function) {
        functionMap.put(function.getName().toLowerCase(Locale.ENGLISH), function);
    }

    @Override
    public IFunction getFunction(String name) {
        return functionMap.get(name.toLowerCase(Locale.ENGLISH));
    }

}
