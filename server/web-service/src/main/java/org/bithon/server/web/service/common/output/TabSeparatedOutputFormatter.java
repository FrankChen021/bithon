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

import org.apache.calcite.linq4j.Enumerable;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.bithon.server.web.service.common.output.IOutputFormatter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * @author Frank Chen
 * @date 4/3/23 3:48 pm
 */
public class TabSeparatedOutputFormatter implements IOutputFormatter {

    @Override
    public void format(PrintWriter writer, List<RelDataTypeField> fields, Enumerable<Object[]> rows) throws IOException {
        if (rows.any()) {
            for (RelDataTypeField field : fields) {
                writer.write(field.getName());
                writer.write('\t');
            }
        }
        writer.write('\n');
        for (Object[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                Object cell = row[i];
                if (cell == null) {
                    writer.write("NULL");
                } else {
                    String c = cell.toString();
                    if (c.indexOf('\n') > 0) {
                        StringBuilder indent = new StringBuilder("\n");
                        for (int j = 0; j < i; j++) {
                            indent.append('\t');
                        }
                        c = c.replaceAll("\n", indent.toString());
                    }
                    writer.write(c);
                }
                writer.write('\t');
            }
            writer.write('\n');
        }
    }

    @Override
    public String getContentType() {
        return "text/tab-separated-values; charset=UTF-8";
    }
}
