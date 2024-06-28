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

package org.bithon.server.storage.jdbc.clickhouse.common;

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.time.DateTime;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.jdbc.clickhouse.ClickHouseConfig;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.conf.ParamType;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 28/7/22 1:15 PM
 */
@Slf4j
public class DataCleaner {

    private final ClickHouseConfig config;
    private final DSLContext dsl;

    public DataCleaner(ClickHouseConfig config, DSLContext dsl) {
        this.config = config;
        this.dsl = dsl;
    }

    public void deletePartition(String table, Timestamp before) {
        deletePartition(table, before, Collections.emptyList());
    }

    /**
     * DELETE PARTITION is a very lightweight operation
     */
    @SuppressWarnings("unchecked")
    public void deletePartition(String table, Timestamp before, List<TimeSpan> skipDateList) {
        String fromTable;
        if (StringUtils.isEmpty(config.getCluster())) {
            fromTable = "system.parts";
        } else {
            fromTable = StringUtils.format("clusterAllReplicas('%s', system, parts)", config.getCluster());
        }

        Set<String> skipPartitions = skipDateList.stream()
                                                 .map((timestamp) -> DateTime.toYYYYMMDD(timestamp.getMilliseconds()))
                                                 .collect(Collectors.toSet());

        // The part name has a pattern of P1-P2-Pn_min_max_level, where the P is the partition expression.e.g. 20240119-2_262_262_0, 20240119_10503_10503_0
        // Since the timestamp is forced used as the 1st partition expression,
        // we can simply compare the part name and the given date
        String localTable = config.getLocalTableName(table);
        String selectPartitionSql = StringUtils.format(
            "SELECT distinct partition FROM %s WHERE database = '%s' AND table = '%s' AND name < '%s'",
            fromTable,
            config.getDatabase(),
            localTable,
            DateTime.toYYYYMMDD(before.getTime()));

        List<String> partitions = (List<String>) dsl.fetch(selectPartitionSql)
                                                    .getValues(0);

        for (String partition : partitions) {
            if (skipPartitions.contains(partition)) {
                log.info("\tDrop [{}] on [{}] is skipped due to it's configured to be kept", table, partition);
                continue;
            }
            log.info("\tDrop [{}] on [{}]", table, partition);
            dsl.execute(StringUtils.format("ALTER TABLE %s.%s %s DROP PARTITION %s;",
                                           config.getDatabase(),
                                           localTable,
                                           config.getOnClusterExpression(),
                                           partition));
        }
    }

    /**
     * Delete data from table is a heavy operation in ClickHouse
     *
     */
    public void deleteByCondition(Table<?> table, Condition condition) {
        // Old CK does not support qualified name in the WHERE
        //noinspection deprecation
        String conditionText = dsl.renderContext()
                                  .qualify(false)
                                  .paramType(ParamType.INLINED)
                                  .visit(condition)
                                  .render();

        deleteByCondition(table, conditionText, -1);
    }

    /**
     * TODO: Check if DELETE statement is supported, if supported, use it
     */
    public void deleteByCondition(Table<?> table, String condition, int deleteCountThreshold) {
        if (deleteCountThreshold > 0) {
            long rowCount = dsl.fetchOne(StringUtils.format("SELECT count(1) FROM %s.%s WHERE %s",
                                                            config.getDatabase(),
                                                            table.getName(),
                                                            condition))
                               .getValue(0, Long.class);
            if (rowCount < deleteCountThreshold) {
                log.info("DELETE on table [{}] is skipped because only [{}] rows matches which is lower than the given threshold [{}].",
                         table.getName(),
                         rowCount,
                         deleteCountThreshold);
                return;
            }
        }

        dsl.execute(StringUtils.format("ALTER TABLE %s.%s %s DELETE WHERE %s",
                                       config.getDatabase(),
                                       config.getLocalTableName(table.getName()),
                                       config.getOnClusterExpression(),
                                       condition));
    }
}
