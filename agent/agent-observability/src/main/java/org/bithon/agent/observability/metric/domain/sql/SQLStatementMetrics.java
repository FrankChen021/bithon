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

package org.bithon.agent.observability.metric.domain.sql;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/5 9:20 下午
 */
public class SQLStatementMetrics {
    private final String driverType;
    private String sql;
    private final AtomicLong executeCount = new AtomicLong(0);
    private final AtomicLong executeErrorCount = new AtomicLong(0);
    private final AtomicLong totalTime = new AtomicLong(0);
    private final LongAccumulator lastTime = new LongAccumulator(Long::max, 0L);
    private final LongAccumulator maxTimeSpan = new LongAccumulator(Long::max, 0L);
    private final AtomicLong effectedRowCount = new AtomicLong();
    private final AtomicLong fetchedRowCount = new AtomicLong();
    private final LongAccumulator batchSizeMax = new LongAccumulator(Long::max, 0L);
    private final AtomicLong batchSizeTotal = new AtomicLong();
    private final LongAccumulator concurrentMax = new LongAccumulator(Long::max, 0L);

    public SQLStatementMetrics(String driverType) {
        this.driverType = driverType;
    }

    public SQLStatementMetrics add(long executeNum,
                                   long executeErrorNum,
                                   long executeTime) {
        executeCount.addAndGet(executeNum);
        executeErrorCount.addAndGet(executeErrorNum);
        totalTime.addAndGet(executeTime);
        lastTime.accumulate(System.currentTimeMillis());
        maxTimeSpan.accumulate(executeTime);

        /*
         * effectedRowCount.addAndGet(appender.effectedRowCount.get());
         * fetchedRowCount.addAndGet(appender.fetchedRowCount.get());
         * batchSizeMax.accumulate(appender.batchSizeMax.get());
         * batchSizeTotal.addAndGet(appender.batchSizeTotal.get());
         * concurrentMax.accumulate(appender.concurrentMax.get());
         */

        return this;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }
}
