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

package org.bithon.component.commons.expression.function;

import org.bithon.component.commons.expression.function.builtin.AggregateFunction;
import org.bithon.component.commons.expression.function.builtin.NumberFunction;
import org.bithon.component.commons.expression.function.builtin.StringFunction;
import org.bithon.component.commons.expression.function.builtin.TimeFunction;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private final Map<String, IFunction> functionMap = new ConcurrentHashMap<>(17);

    /**
     * <p>
     * <a href="http://h2database.com/html/functions.html">H2 Functions</a>
     * <p>
     * <a href="https://clickhouse.com/docs/en/sql-reference/functions/string-functions">ClickHouse Functions</a>
     */
    public Functions() {
        register(new NumberFunction.Round());

        register(StringFunction.StartsWith.INSTANCE);
        register(StringFunction.EndsWith.INSTANCE);
        register(StringFunction.HasToken.INSTANCE);
        register(new StringFunction.Lower());
        register(new StringFunction.Upper());
        register(new StringFunction.Substring());
        register(new StringFunction.Trim());
        register(new StringFunction.TrimLeft());
        register(new StringFunction.TrimRight());
        register(new StringFunction.Length());
        register(new StringFunction.Concat());

        register(new TimeFunction.ToStartOfMinute());
        register(new TimeFunction.Now());
        register(TimeFunction.ToMilliSeconds.INSTANCE);
        register(TimeFunction.ToMicroSeconds.INSTANCE);
        register(TimeFunction.ToNanoSeconds.INSTANCE);
        register(TimeFunction.FromUnixTimestamp.INSTANCE);

        register(AggregateFunction.Min.INSTANCE);
        register(AggregateFunction.Max.INSTANCE);
        register(AggregateFunction.Sum.INSTANCE);
        register(AggregateFunction.Count.INSTANCE);
        register(AggregateFunction.Avg.INSTANCE);
        register(AggregateFunction.First.INSTANCE);
        register(AggregateFunction.Last.INSTANCE);
    }

    public void register(IFunction function) {
        functionMap.put(function.getName().toLowerCase(Locale.ENGLISH), function);
    }

    @Override
    public IFunction getFunction(String name) {
        return functionMap.get(name.toLowerCase(Locale.ENGLISH));
    }

}
