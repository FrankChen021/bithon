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

package org.bithon.agent.plugin.thread.metrics;

import org.bithon.agent.core.metric.domain.thread.ThreadPoolMetrics;
import org.bithon.agent.core.metric.model.IMetricValueProvider;

import java.util.concurrent.ForkJoinPool;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/25 11:26 下午
 */
public class ForkJoinPoolMetrics extends ThreadPoolMetrics {

    public ForkJoinPoolMetrics(ForkJoinPool pool) {
        super(pool::getActiveThreadCount,
              pool::getPoolSize,
              pool::getParallelism,
              new IMetricValueProvider() {
                  private long largestPoolSize = 0;

                  @Override
                  public long get() {
                      largestPoolSize = Math.max(pool.getPoolSize(), largestPoolSize);
                      return largestPoolSize;
                  }
              },
              pool::getQueuedTaskCount);
    }
}
