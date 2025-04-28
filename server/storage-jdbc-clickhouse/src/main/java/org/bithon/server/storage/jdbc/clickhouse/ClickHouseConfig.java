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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.bithon.component.commons.utils.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 1/11/21 4:54 pm
 */
@Data
public class ClickHouseConfig {
    // JDBC url
    private String url;
    private String username;
    private String password;

    private String cluster;
    private boolean onDistributedTable = false;

    /**
     * MergeTree
     * or for example ReplicatedMergeTree('/clickhouse/tables/{layer}-{shard}/{database}.{table}','{replica}')
     */
    private String engine = "MergeTree";

    @Data
    public static class SecondaryPartition {
        /**
         * The name of the column that is used as secondary partition
         */
        private String column;

        /**
         * The size of secondary partition
         */
        private int count;
    }


    /**
     * key is the name of table which needs to be partitioned
     */
    private Map<String, SecondaryPartition> secondaryPartitions = Collections.emptyMap();

    /**
     * Even the data lifecycle is managed by ourselves, we still need this property in case of customizing TTL MOVE.
     * For example: TTL toStartOfHour(timestamp) + toIntervalDay(1) TO VOLUME 'cold'
     */
    private String ttl;

    /**
     * Settings for create table
     */
    private String createTableSettings;

    @JsonIgnore
    private String database;

    /**
     * a runtime property
     */
    private String tableEngine;

    public void afterPropertiesSet() throws URISyntaxException {
        if (!StringUtils.hasText(engine)) {
            throw new RuntimeException("'engine' should not be null");
        }

        if (this.onDistributedTable && StringUtils.isEmpty(cluster)) {
            throw new RuntimeException("Config to use distributed table but 'cluster' is not specified.");
        }

        int parenthesesIndex = engine.indexOf('(');
        tableEngine = (parenthesesIndex == -1 ? engine : engine.substring(0, parenthesesIndex)).trim();
        if (!tableEngine.endsWith("MergeTree")) {
            throw new RuntimeException(StringUtils.format("engine [%s] is not a member of MergeTree family", tableEngine));
        }
        if (tableEngine.startsWith("ReplicatedMergeTree") && !StringUtils.hasText(cluster)) {
            throw new RuntimeException("ReplicatedMergeTree requires cluster to be given");
        }

        if (!url.startsWith("jdbc:")) {
            throw new RuntimeException("jdbc format is wrong.");
        }
        URI uri = new URI(url.substring("jdbc:".length()));
        this.database = uri.getPath().substring(1);
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
