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

package org.bithon.server.storage.jdbc.clickhouse;

import lombok.Data;
import org.bithon.component.commons.utils.StringUtils;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author frank.chen021@outlook.com
 * @date 1/11/21 4:54 pm
 */
@Data
public class ClickHouseConfig implements InitializingBean {
    private String cluster;
    private boolean onDistributedTable = false;
    private String engine = "MergeTree";
    private String database;
    private int ttlDays = 7;

    /**
     * a runtime property
     */
    private String tableEngine;

    @Override
    public void afterPropertiesSet() {
        if (!StringUtils.hasText(engine)) {
            throw new RuntimeException("'engine' should not be null");
        }

        if (this.onDistributedTable && StringUtils.isEmpty(cluster)) {
            throw new RuntimeException("Config to use distributed table but 'cluster' is not specified.");
        }

        int spaceIndex = engine.indexOf(' ');
        tableEngine = spaceIndex == -1 ? engine : engine.substring(0, spaceIndex);
        if (!tableEngine.endsWith("MergeTree")) {
            throw new RuntimeException(StringUtils.format("engine [%s] is not a member of MergeTree family", tableEngine));
        }
        if (tableEngine.startsWith("ReplicatedMergeTree") && !StringUtils.hasText(cluster)) {
            throw new RuntimeException("ReplicatedMergeTree requires cluster to be given");
        }
    }

    public String getLocalTableName(String tableName) {
        if (onDistributedTable) {
            return tableName + "_local";
        } else {
            return tableName;
        }
    }

    public String getOnClusterExpression() {
        if (StringUtils.hasText(this.cluster)) {
            return " ON CLUSTER " + this.cluster + " ";
        } else {
            return "";
        }
    }
}
