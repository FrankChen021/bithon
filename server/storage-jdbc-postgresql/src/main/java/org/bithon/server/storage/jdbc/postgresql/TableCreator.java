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

package org.bithon.server.storage.jdbc.postgresql;

import org.jooq.DSLContext;
import org.jooq.Index;
import org.jooq.Table;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/12/29 23:13
 */
public class TableCreator {
    public static void createTableIfNotExists(DSLContext dslContext, Table<?> table) {
        dslContext.createTableIfNotExists(table)
                  .columns(table.fields())
                  .execute();

        for (Index index : table.getIndexes()) {
            dslContext.createIndexIfNotExists(index)
                      .on(table, index.getFields())
                      .execute();
        }
    }
}
