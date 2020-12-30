package com.sbss.bithon.agent.plugin.quartz2;

import com.keruyun.commons.agent.collector.entity.JobTriggerEntity;
import com.keruyun.commons.agent.collector.entity.QuartzEntity;
import com.keruyun.commons.agent.collector.entity.QuartzInfoEntity;
import com.sbss.bithon.agent.core.interceptor.AbstractMethodIntercepted;
import com.sbss.bithon.agent.dispatcher.metrics.DispatchProcessor;
import org.quartz.SchedulerException;
import org.quartz.impl.SchedulerRepository;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Description : quartz2.x监控插件, 同时采集quartz2 job list和job counts <br>
 * Date: 17/9/4
 *
 * @author 马至远
 */
public class QuartzHandler extends AbstractMethodIntercepted {
    private static final Logger log = LoggerFactory.getLogger(QuartzHandler.class);

    private DispatchProcessor dispatchProcessor;

    private Quartz2Monitor quartzMonitor;

    private int checkPeriod;

    @Override
    public boolean init() throws Exception {
        if (Quartz2Monitor.isQuartz1()) {
            // 非2.x 版本直接忽略直接退出
            log.info("quartz version do not support");
            return false;
        }

        checkPeriod = 10;

        dispatchProcessor = DispatchProcessor.getInstance();

        // try using static method to get the instance status
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                dispatch();
            }
        }, 0, checkPeriod * 1000);

        return true;
    }

    @Override
    public void onConstruct(Object constructedObject,
                            Object[] args) throws Exception {
        // 尝试获取quartz依赖
        quartzMonitor = Quartz2Monitor.getQuartz2MonitorInstance((SchedulerRepository) constructedObject);

    }

    /**
     * 构建job counts 数据
     *
     * @return jobCountsEntity
     * @throws SchedulerException 调度器异常
     */
    private QuartzEntity buildJobCountsEntity() throws SchedulerException {
        QuartzEntity q = quartzMonitor.buildQuartzData();

        // 包装App信息
        q.setAppName(dispatchProcessor.getAppName());
        q.setHostName(dispatchProcessor.getIpAddress());
        q.setPort(dispatchProcessor.getPort());
        q.setCollectCount(1);
        q.setTimestamp(System.currentTimeMillis());
        q.setInterval(checkPeriod);

        return q;
    }

    /**
     * 构造job list 数据
     *
     * @return jobListEntity
     * @throws SchedulerException 调度器异常
     */
    private QuartzInfoEntity buildJobListEntity() throws SchedulerException {
        List<JobTriggerEntity> jobTriggerEntityList = quartzMonitor.buildQuartJobList();

        QuartzInfoEntity quartzInfoEntity = new QuartzInfoEntity();

        // 包装App信息
        quartzInfoEntity.setAppName(dispatchProcessor.getAppName());
        quartzInfoEntity.setHostName(dispatchProcessor.getIpAddress());
        quartzInfoEntity.setPort(dispatchProcessor.getPort());
        quartzInfoEntity.setJobTriggerList(jobTriggerEntityList);

        return quartzInfoEntity;
    }

    private void dispatch() {
        try {
            if (dispatchProcessor.ready) {
                dispatchProcessor.pushMessage(buildJobCountsEntity());
                QuartzInfoEntity quartzInfoEntity = buildJobListEntity();
                if (!quartzInfoEntity.getJobTriggerList().isEmpty()) {
                    dispatchProcessor.pushMessage(quartzInfoEntity);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
