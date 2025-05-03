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

package org.bithon.server.datasource.reader.vm;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.function.IFunction;
import org.bithon.server.commons.time.Period;
import org.bithon.server.datasource.ISchema;
import org.bithon.server.datasource.TimestampSpec;
import org.bithon.server.datasource.column.AbstractColumn;
import org.bithon.server.datasource.column.IColumn;
import org.bithon.server.datasource.query.ast.Selector;
import org.bithon.server.datasource.store.IDataStoreSpec;

import java.util.Collection;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 28/4/25 10:38 pm
 */
public class VMSchema implements ISchema {
    private final String name;
    private final TimestampSpec timestampSpec;
    private final VMDataStoreSpec dataStoreSpec;
    private String signature;

    static class Column extends AbstractColumn {
        public Column(String name, String alias) {
            super(name, alias);
        }

        @Override
        public IExpression createAggregateFunctionExpression(IFunction function) {
            return new FunctionExpression(function, IdentifierExpression.of(getName(), IDataType.DOUBLE));
        }

        @Override
        public IDataType getDataType() {
            return IDataType.STRING;
        }

        @Override
        public Selector toSelector() {
            return new Selector(getName(), getDataType());
        }
    }

    @JsonCreator
    public VMSchema(@JsonProperty("name") String name,
                    @JsonProperty("dataStoreSpec") VMDataStoreSpec dataStoreSpec) {
        this.name = name;
        this.timestampSpec = new TimestampSpec(TimestampSpec.DEFAULT_COLUMN);
        this.dataStoreSpec = dataStoreSpec;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDisplayText() {
        return "";
    }

    @JsonIgnore
    @Override
    public TimestampSpec getTimestampSpec() {
        return this.timestampSpec;
    }

    @Override
    public IColumn getColumnByName(String name) {
        return new Column(name, name);
    }

    @JsonIgnore
    @Override
    public Collection<IColumn> getColumns() {
        return List.of();
    }

    @JsonIgnore
    @Override
    public JsonNode getInputSourceSpec() {
        return null;
    }

    @Override
    public IDataStoreSpec getDataStoreSpec() {
        return this.dataStoreSpec;
    }

    @Override
    public ISchema withDataStore(IDataStoreSpec spec) {
        return new VMSchema(name, dataStoreSpec);
    }

    @Override
    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String getSignature() {
        return this.signature;
    }

    @JsonIgnore
    @Override
    public Period getTtl() {
        return null;
    }
}
