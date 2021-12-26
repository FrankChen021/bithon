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

package org.bithon.agent.plugin.quartz2;

import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.quartz.impl.SchedulerRepository;

public class Quartz2Monitor {
    private static final ILogAdaptor log = LoggerFactory.getLogger(Quartz2Monitor.class);

    private static SchedulerRepository schedulerRepository;

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

    private static class Quartz2MonitorHandler {
        static final Quartz2Monitor INSTANCE = new Quartz2Monitor();
    }

    /*
    public QuartzEntity buildQuartzData() throws SchedulerException {
        int numJobsTotal = 0;
        int numJobsExecuted = 0;
        int numJobsCurrentlyExecuting = 0;
        int numJobsPause = 0;
        int numJobsBlock = 0;
        int numJobsFail = 0;

        QuartzEntity quartzEntity = new QuartzEntity();

        List<Scheduler> schedulerList = new ArrayList<>(schedulerRepository.lookupAll());
        for (Scheduler scheduler : schedulerList) {

            numJobsExecuted += scheduler.getMetaData().getNumberOfJobsExecuted();

            numJobsCurrentlyExecuting += scheduler.getCurrentlyExecutingJobs().size();

            for (String jobGroup : scheduler.getJobGroupNames()) {
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(jobGroup))) {
                    numJobsTotal++;

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
    }*/
}
