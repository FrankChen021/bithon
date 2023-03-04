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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * @author Frank Chen
 * @date 4/3/23 3:47 pm
 */
public interface IOutputFormatter {
    void format(PrintWriter writer, List<RelDataTypeField> fields, Enumerable<Object[]> rows) throws IOException;

    String getContentType();
}
