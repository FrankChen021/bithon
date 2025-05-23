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

package org.bithon.server.web.service.common.calcite;

import org.apache.calcite.DataContext;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.jdbc.CalciteSchema;
import org.apache.calcite.linq4j.QueryProvider;
import org.apache.calcite.schema.SchemaPlus;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 1/3/23 12:00 pm
 */
public final class SqlExecutionContext implements DataContext {
    private final SchemaPlus schema;
    private final JavaTypeFactory typeFactory;

    /**
     * mutable parameters
     */
    private final Map<String, Object> parameters = new HashMap<>();

    SqlExecutionContext(CalciteSchema calciteSchema, JavaTypeFactory typeFactory) {
        this.schema = calciteSchema.plus();
        this.typeFactory = typeFactory;
    }

    @Override
    public SchemaPlus getRootSchema() {
        return schema;
    }

    @Override
    public JavaTypeFactory getTypeFactory() {
        return typeFactory;
    }

    @Override
    public QueryProvider getQueryProvider() {
        return null;
    }

    @Override
    public Object get(final String name) {
        return parameters.get(name);
    }

    public void set(String name, Object val) {
        parameters.put(name, val);
    }

    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }
}
