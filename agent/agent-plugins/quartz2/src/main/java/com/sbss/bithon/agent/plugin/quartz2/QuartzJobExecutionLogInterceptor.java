package com.sbss.bithon.agent.plugin.quartz2;

import com.sbss.bithon.agent.core.metrics.MetricProviderManager;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * @author frankchen
 */
public class QuartzJobExecutionLogInterceptor extends AbstractInterceptor {
    private static final Logger log = LoggerFactory.getLogger(QuartzJobExecutionLogInterceptor.class);

    private QuartzMetricProvider quartzCounter;

    @Override
    public boolean initialize() {
        if (Quartz2Monitor.isQuartz1()) {
            log.info("quartz version do not support");
            return false;
        }

        quartzCounter = (QuartzMetricProvider)
            MetricProviderManager.getInstance().register(QuartzMetricProvider.COUNTER_NAME, new QuartzMetricProvider());

        return true;
    }

    @Override
    public void onMethodLeave(AopContext context) {
        quartzCounter.update(context);
    }
}
