package com.sbss.bithon.agent.plugin.quartz2;

import com.keruyun.commons.agent.collector.entity.QuartzInfoEntity;
import com.sbss.bithon.agent.core.interceptor.AfterJoinPoint;
import com.sbss.bithon.agent.dispatcher.metrics.counter.IAgentCounter;
import org.quartz.core.JobRunShell;
import org.quartz.impl.JobExecutionContextImpl;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Description : http客户端 - okhttp3记录器 <br>
 * Date: 17/10/30
 *
 * @author 马至远
 */
public class QuartzCounter implements IAgentCounter {
    private static final Logger log = LoggerFactory.getLogger(QuartzCounter.class);

    static final String COUNTER_NAME = "quartz2";

    private static final String JOB_NAME = "jobName";
    private static final String JOB_CLASS = "jobClass";
    private static final String JOB_START_TIME = "jobStartTime";
    private static final String JOB_FINISH_TIME = "jobFinishTime";
    private static final String DURATION = "duration";
    private static final String JOB_EXECUTE_RESULT = "jobExecuteResult";
    private static final String JOB_SUCCESS = "SUCCESS";
    private static final String JOB_FAIL = "FAIL";

    private Queue<Map<String, String>> quartzLogs;

    QuartzCounter() {
        this.quartzLogs = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void add(Object o) {
        Map<String, String> quartzLog = new HashMap<>(6);

        AfterJoinPoint joinPoint = (AfterJoinPoint) o;
        long startTime = (long) joinPoint.getContext();
        long endTime = System.currentTimeMillis();
        JobRunShell jobRunShell = (JobRunShell) joinPoint.getTarget();

        // 反射式的获取jobExecutionContext, 是不使用listener情况下的统一入口
        try {
            Field field = jobRunShell.getClass().getDeclaredField("jec");
            field.setAccessible(true);
            JobExecutionContextImpl jobExecutionContext = (JobExecutionContextImpl) field.get(jobRunShell);

            quartzLog.put(JOB_NAME, jobExecutionContext.getJobDetail().getKey().toString());
            quartzLog.put(JOB_CLASS, jobExecutionContext.getJobDetail().getJobClass().getName());
            quartzLog.put(JOB_START_TIME, String.valueOf(startTime));
            quartzLog.put(JOB_FINISH_TIME, String.valueOf(endTime));
            quartzLog.put(DURATION, String.valueOf(jobExecutionContext.getJobRunTime()));
            quartzLog.put(JOB_EXECUTE_RESULT, null == joinPoint.getException() ? JOB_SUCCESS : JOB_FAIL);

            quartzLogs.add(quartzLog);
        } catch (NoSuchFieldException e) {
            log.warn("quartz jobContext field missing");
        } catch (IllegalAccessException e) {
            log.error("quartz jobContext field mismatch");
        }
    }

    @Override
    public boolean isEmpty() {
        return quartzLogs.isEmpty();
    }

    @Override
    public List buildAndGetThriftEntities(int interval,
                                          String appName,
                                          String ipAddress,
                                          int port) {
        return buildEntities(interval, appName, ipAddress, port);
    }

    /**
     * 从当前storage中构建thrift数据
     *
     * @return agent采集数据
     */
    private List<QuartzInfoEntity> buildEntities(int interval,
                                                 String appName,
                                                 String ipAddress,
                                                 int port) {
        List<Map<String, String>> logs = new ArrayList<>(quartzLogs);
        quartzLogs.clear();
        QuartzInfoEntity quartzInfoEntity = new QuartzInfoEntity(null, logs);
        quartzInfoEntity.setAppName(appName);
        quartzInfoEntity.setHostName(ipAddress);
        quartzInfoEntity.setPort(port);

        return Collections.singletonList(quartzInfoEntity);
    }
}
