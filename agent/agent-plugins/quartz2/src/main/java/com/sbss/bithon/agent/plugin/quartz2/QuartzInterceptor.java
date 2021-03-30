package com.sbss.bithon.agent.plugin.quartz2;

import com.sbss.bithon.agent.core.dispatcher.Dispatcher;
import com.sbss.bithon.agent.core.dispatcher.Dispatchers;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AbstractInterceptor;
import com.sbss.bithon.agent.core.plugin.aop.bootstrap.AopContext;
import org.quartz.impl.SchedulerRepository;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author frankchen
 */
public class QuartzInterceptor extends AbstractInterceptor {
    private static final Logger log = LoggerFactory.getLogger(QuartzInterceptor.class);

    private Dispatcher dispatcher;

    private Quartz2Monitor quartzMonitor;

    private int checkPeriod;

    @Override
    public boolean initialize() throws Exception {
        if (Quartz2Monitor.isQuartz1()) {
            log.info("quartz version do not support");
            return false;
        }

        checkPeriod = 10;

        dispatcher = Dispatchers.getOrCreate(Dispatchers.DISPATCHER_NAME_METRIC);

        // try using static method to get the instance status
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                dispatch();
            }
        }, 0, checkPeriod * 1000L);

        return true;
    }

    @Override
    public void onConstruct(AopContext aopContext) {
        quartzMonitor = Quartz2Monitor.getQuartz2MonitorInstance((SchedulerRepository) aopContext.castTargetAs());
    }

//    private QuartzEntity buildJobCountsEntity() throws SchedulerException {
//        QuartzEntity q = quartzMonitor.buildQuartzData();
//
//        q.setAppName(dispatcher.getAppName());
//        q.setHostName(dispatcher.getIpAddress());
//        q.setPort(dispatcher.getPort());
//        q.setCollectCount(1);
//        q.setTimestamp(System.currentTimeMillis());
//        q.setInterval(checkPeriod);
//
//        return q;
//    }

//    private QuartzInfoEntity buildJobListEntity() throws SchedulerException {
//        List<JobTriggerEntity> jobTriggerEntityList = quartzMonitor.buildQuartJobList();
//
//        QuartzInfoEntity quartzInfoEntity = new QuartzInfoEntity();
//
//        quartzInfoEntity.setAppName(dispatcher.getAppName());
//        quartzInfoEntity.setHostName(dispatcher.getIpAddress());
//        quartzInfoEntity.setPort(dispatcher.getPort());
//        quartzInfoEntity.setJobTriggerList(jobTriggerEntityList);
//
//        return quartzInfoEntity;
//    }

    private void dispatch() {
        try {
            if (dispatcher.isReady()) {
//                dispatcher.sendMetrics(buildJobCountsEntity());
//                QuartzInfoEntity quartzInfoEntity = buildJobListEntity();
//                if (!quartzInfoEntity.getJobTriggerList().isEmpty()) {
//                    dispatcher.sendMetrics(quartzInfoEntity);
//                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
