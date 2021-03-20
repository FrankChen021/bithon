package com.sbss.bithon.agent.plugin.quartz2;

import com.sbss.bithon.agent.core.metric.MetricCollectorManager;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
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
