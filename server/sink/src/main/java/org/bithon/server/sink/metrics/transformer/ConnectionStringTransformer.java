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

package org.bithon.server.sink.metrics.transformer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Getter;
import org.bithon.server.commons.utils.DbUtils;
import org.bithon.server.storage.datasource.input.IInputRow;
import org.bithon.server.storage.datasource.input.transformer.ITransformer;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/8/9 20:49
 */
@JsonTypeName("connectionString")
public class ConnectionStringTransformer implements ITransformer {

    @Getter
    private final String field;

    @JsonCreator
    public ConnectionStringTransformer(@JsonProperty("field") String field) {
        this.field = field == null ? "connectionString" : field;
    }

    @Override
    public void transform(IInputRow inputRow) throws TransformException {
        DbUtils.ConnectionString conn = DbUtils.parseConnectionString(inputRow.getColAsString(field));
        inputRow.updateColumn("server", conn.getHostAndPort());
        inputRow.updateColumn("database", conn.getDatabase());
        inputRow.updateColumn("endpointType", conn.getDbType());
    }
}
