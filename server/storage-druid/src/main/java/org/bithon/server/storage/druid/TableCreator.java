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

package org.bithon.server.storage.druid;

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.SQLDataType;

/**
 * @author frank.chen021@outlook.com
 * @date 1/11/21 6:48 pm
 */
@Slf4j
public class TableCreator {

    private final DruidConfig config;
    private final DSLContext dslContext;

    public TableCreator(DruidConfig config, DSLContext dslContext) {
        this.config = config;
        this.dslContext = dslContext;
    }

    private boolean useReplacingMergeTree = false;

    /**
     * the version field of a ReplacingMergeTree
     */
    private String replacingMergeTreeVersion = "timestamp";

    private String partitionByExpression = "toYYYYMMDD(timestamp)";

    public TableCreator useReplacingMergeTree(boolean useReplacingMergeTree) {
        this.useReplacingMergeTree = useReplacingMergeTree;
        return this;
    }

    public TableCreator replacingMergeTreeVersion(String replacingMergeTreeVersion) {
        this.replacingMergeTreeVersion = replacingMergeTreeVersion;
        return this;
    }

    public TableCreator partitionByExpression(String partitionByExpression) {
        this.partitionByExpression = partitionByExpression;
        return this;
    }

    public void createIfNotExist(Table<?> table) {
        // TODO: ensure the kafka-supervisor-spec
    }

    private String getFieldText(Table<?> table) {
        StringBuilder sb = new StringBuilder(128);
        for (Field<?> f : table.fields()) {
            if (f.getDataType().equals(SQLDataType.TIMESTAMP)) {
                sb.append(StringUtils.format("`%s` %s(3,0) ,%n",
                                             f.getName(),
                                             f.getDataType().getTypeName()));
                continue;
            }
            if (f.getDataType().hasPrecision()) {
                sb.append(StringUtils.format("`%s` %s(%d, %d) ,%n",
                                             f.getName(),
                                             f.getDataType().getTypeName(),
                                             f.getDataType().precision(),
                                             f.getDataType().scale()));
            } else {
                sb.append(StringUtils.format("`%s` %s ,%n", f.getName(), f.getDataType().getTypeName()));
            }
        }
        sb.delete(sb.length() - 2, sb.length());
        return sb.toString();
    }
}
