/*
 *    Copyright 2020 bithon.cn
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

package org.bithon.agent.plugin.quartz2;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.core.metric.collector.MetricCollectorManager;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author frankchen
 */
public class QuartzJobExecutionLogInterceptor extends AbstractInterceptor {
    private static final Logger log = LoggerFactory.getLogger(QuartzJobExecutionLogInterceptor.class);

    private QuartzMetricCollector quartzCounter;

    @Override
    public boolean initialize() {
        if (Quartz2Monitor.isQuartz1()) {
            log.info("quartz version do not support");
            return false;
        }

        quartzCounter = MetricCollectorManager.getInstance()
                                              .getOrRegister(QuartzMetricCollector.COUNTER_NAME,
                                                             QuartzMetricCollector.class);

        return true;
    }

    @Override
    public void onMethodLeave(AopContext context) {
        quartzCounter.update(context);
    }
}
