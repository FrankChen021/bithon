package com.sbss.bithon.agent.plugin.quartz2;

import com.keruyun.commons.agent.collector.entity.JobTriggerEntity;
import com.keruyun.commons.agent.collector.entity.QuartzEntity;
import org.quartz.*;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.SchedulerRepository;
import org.quartz.impl.matchers.GroupMatcher;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Description : Quartz 2.x 版本的数据采集实现 <br>
 * Date: 17/9/6
 *
 * @author 马至远
 */
public class Quartz2Monitor {
    private static final Logger log = LoggerFactory.getLogger(Quartz2Monitor.class);

    private static SchedulerRepository schedulerRepository;

    private static class Quartz2MonitorHandler {
        static final Quartz2Monitor INSTANCE = new Quartz2Monitor();
    }

    private Quartz2Monitor() {
    }

    public static Quartz2Monitor getQuartz2MonitorInstance(SchedulerRepository s) {
        Quartz2Monitor quartz2Monitor = Quartz2MonitorHandler.INSTANCE;
        setSchedulerRepository(s);

        return quartz2Monitor;
    }

    private static void setSchedulerRepository(SchedulerRepository s) {
        Quartz2Monitor.schedulerRepository = s;
    }

    /**
     * 判断当前的Quartz版本, 分别进行数据处理
     *
     * @return 是否是Quartz-2.x版本
     */
    public static boolean isQuartz1() {
        try {
            Class.forName("org.quartz.JobKey");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }

    public QuartzEntity buildQuartzData() throws SchedulerException {
        // 任务总数
        int numJobsTotal = 0;
        // 已执行任务数
        int numJobsExecuted = 0;
        // 正在执行任务数
        int numJobsCurrentlyExecuting = 0;
        // 暂停trigger数
        int numJobsPause = 0;
        // 阻塞trigger数
        int numJobsBlock = 0;
        // 失败trigger数
        int numJobsFail = 0;

        QuartzEntity quartzEntity = new QuartzEntity();

        List<Scheduler> schedulerList = new ArrayList<>(schedulerRepository.lookupAll());
        // 遍历schedulerRepository中的数据, 在不同的层次分别取出需要的值
        for (Scheduler scheduler : schedulerList) {

            // 当前scheduler已执行任务数
            numJobsExecuted += scheduler.getMetaData().getNumberOfJobsExecuted();

            // 当前scheduler正在执行任务数
            numJobsCurrentlyExecuting += scheduler.getCurrentlyExecutingJobs().size();

            for (String jobGroup : scheduler.getJobGroupNames()) {
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(jobGroup))) {
                    numJobsTotal++; // 总任务数

                    // 以job维度, 来统计triggers的状态 -> job执行状态
                    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                    for (Trigger trigger : triggers) {
                        Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());

                        switch (triggerState) {
                            case NONE:
                            case ERROR:
                                numJobsFail++;
                                break;
                            case PAUSED:
                                numJobsPause++;
                                break;
                            case BLOCKED:
                                numJobsBlock++;
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }

        quartzEntity.setJobsCurrentlyExecuting(numJobsCurrentlyExecuting);
        quartzEntity.setJobsExecuted(numJobsExecuted);
        quartzEntity.setJobsTotal(numJobsTotal);
        quartzEntity.setJobsBlock(numJobsBlock);
        quartzEntity.setJobsPause(numJobsPause);
        quartzEntity.setJobsFail(numJobsFail);

        return quartzEntity;
    }

    public List<JobTriggerEntity> buildQuartJobList() throws SchedulerException {
        List<JobTriggerEntity> jobTriggerEntityList = new ArrayList<>();

        List<Scheduler> schedulerList = new ArrayList<>(schedulerRepository.lookupAll());

        // 遍历scheduler获取job list
        for (Scheduler scheduler : schedulerList) {
            for (String jobGroup : scheduler.getJobGroupNames()) {
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(jobGroup))) {
                    parseJobDetail(jobTriggerEntityList, scheduler, jobKey);
                }
            }
        }

        return jobTriggerEntityList;
    }

    private void parseJobDetail(List<JobTriggerEntity> jobTriggerEntityList,
                                Scheduler scheduler,
                                JobKey jobKey) throws SchedulerException {
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
        if (null == triggers || triggers.size() <= 0) {
            return;
        }
        JobDetail jobDetail;

        // 检测仓库中job的时候, job可能为冗余数据而取不到class, 此时会抛出异常, 记录为warn级别, 继续遍历其他job
        try {
            jobDetail = scheduler.getJobDetail(jobKey);
        } catch (Exception e) {
            log.warn("Exception caught when building quartz job list, please check: ", e);
            return;
        }

        String jobName;
        if (jobDetail instanceof JobDetailImpl) {
            jobName = ((JobDetailImpl) jobDetail).getName();
        } else {
            jobName = "unknown";
        }

        Trigger trigger = triggers.get(0);
        Trigger.TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());

        String cronExpression;
        if (trigger instanceof CronTrigger) {
            cronExpression = ((CronTrigger) trigger).getCronExpression();
        } else if (trigger instanceof SimpleTrigger) {
            cronExpression = "simple interval : " + String.valueOf(((SimpleTrigger) trigger).getRepeatInterval());
        } else {
            cronExpression = "unreachable";
        }

        long prevFireTime = trigger.getPreviousFireTime() == null ? -1 : trigger.getPreviousFireTime().getTime();
        long nextFireTime = trigger.getNextFireTime() == null ? -1 : trigger.getNextFireTime().getTime();

        // 组装当前job的统计数据
        JobTriggerEntity jobTriggerEntity = new JobTriggerEntity(jobName,
                                                                 jobDetail.getKey().getName(),
                                                                 jobDetail.getDescription(),
                                                                 jobDetail.getKey().getGroup(),
                                                                 jobDetail.getJobClass().getName(),
                                                                 cronExpression,
                                                                 triggerState.toString(),
                                                                 prevFireTime,
                                                                 nextFireTime,
                                                                 jobDetail.isDurable(),
                                                                 jobDetail.isPersistJobDataAfterExecution(),
                                                                 jobDetail.isConcurrentExectionDisallowed(),
                                                                 jobDetail.requestsRecovery());
        jobTriggerEntityList.add(jobTriggerEntity);
    }
}
