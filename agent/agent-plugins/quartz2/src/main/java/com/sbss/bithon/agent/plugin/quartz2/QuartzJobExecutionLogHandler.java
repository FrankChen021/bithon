package com.sbss.bithon.agent.plugin.quartz2;

import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.core.interceptor.BeforeJoinPoint;
import com.sbss.bithon.agent.dispatcher.metrics.counter.AgentCounterRepository;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * Description : Quartz 历史纪录采集器 <br>
 * Date: 17/11/16
 *
 * @author 马至远
 */
public class QuartzJobExecutionLogHandler extends AbstractMethodIntercepted {
    private static final Logger log = LoggerFactory.getLogger(QuartzJobExecutionLogHandler.class);

    /**
     * 数据记录器, 用于统计数据
     */
    private IAgentCounter quartzCounter;

    @Override
    public boolean init() throws Exception {
        if (Quartz2Monitor.isQuartz1()) {
            // 非2.x 版本直接忽略直接退出
            log.info("quartz version do not support");
            return false;
        }

        // 获取CounterRepository 实例
        AgentCounterRepository counterRepository = AgentCounterRepository.getInstance();

        // 向counterRepository注册, 开始统计request信息
        quartzCounter = new QuartzCounter();
        counterRepository.register(QuartzCounter.COUNTER_NAME, quartzCounter);

        return true;
    }

    @Override
    protected Object createContext(BeforeJoinPoint joinPoint) {
        return System.currentTimeMillis();
    }

    @Override
    protected void after(AfterJoinPoint joinPoint) {
        quartzCounter.add(joinPoint);
    }
}
