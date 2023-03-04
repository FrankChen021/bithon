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

package org.bithon.server.web.service.common.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.linq4j.Enumerator;
import org.apache.calcite.rel.type.RelDataTypeField;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 4/3/23 3:48 pm
 */
public class JsonCompactOutputFormatter implements IOutputFormatter {
    private final ObjectMapper objectMapper;

    public JsonCompactOutputFormatter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void format(PrintWriter writer, List<RelDataTypeField> fields, Enumerable<Object[]> rows) throws IOException {
        writer.write("{\n");

        String columns = fields.stream().map(field -> "\"" + field.getName() + "\"").collect(Collectors.joining(","));
        writer.format("\"meta\": { \"columns\": [%s] },\n", columns);

        writer.write("\"rows\": [");
        {
            Enumerator<Object[]> e = rows.enumerator();
            if (e.moveNext()) {
                boolean hasNext;
                do {
                    writer.write(objectMapper.writeValueAsString(e.current()));
                    hasNext = e.moveNext();
                    if (hasNext) {
                        writer.write(',');
                    }
                } while (hasNext);
            }
        }
        writer.write("]\n}");
    }

    @Override
    public String getContentType() {
        return "application/json";
    }
}
