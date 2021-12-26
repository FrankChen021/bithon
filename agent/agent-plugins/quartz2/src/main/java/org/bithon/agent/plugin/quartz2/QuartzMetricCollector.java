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

import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.core.dispatcher.IMessageConverter;
import org.bithon.agent.core.metric.collector.IMetricCollector;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.quartz.core.JobRunShell;
import org.quartz.impl.JobExecutionContextImpl;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * @author frankchen
 */
public class QuartzMetricCollector implements IMetricCollector {
    static final String COUNTER_NAME = "quartz2";
    private static final ILogAdaptor log = LoggerFactory.getLogger(QuartzMetricCollector.class);
    private static final String JOB_NAME = "jobName";
    private static final String JOB_CLASS = "jobClass";
    private static final String JOB_START_TIME = "jobStartTime";
    private static final String JOB_FINISH_TIME = "jobFinishTime";
    private static final String DURATION = "duration";
    private static final String JOB_EXECUTE_RESULT = "jobExecuteResult";
    private static final String JOB_SUCCESS = "SUCCESS";
    private static final String JOB_FAIL = "FAIL";

    private Queue<Map<String, String>> quartzLogs;

    public QuartzMetricCollector() {
        this.quartzLogs = new ConcurrentLinkedQueue<>();
    }

    public void update(AopContext context) {
        Map<String, String> quartzLog = new HashMap<>(6);

        JobRunShell jobRunShell = (JobRunShell) context.getTarget();

        // 反射式的获取jobExecutionContext, 是不使用listener情况下的统一入口
        try {
            Field field = jobRunShell.getClass().getDeclaredField("jec");
            field.setAccessible(true);
            JobExecutionContextImpl jobExecutionContext = (JobExecutionContextImpl) field.get(jobRunShell);

            quartzLog.put(JOB_NAME, jobExecutionContext.getJobDetail().getKey().toString());
            quartzLog.put(JOB_CLASS, jobExecutionContext.getJobDetail().getJobClass().getName());
            quartzLog.put(JOB_START_TIME, String.valueOf(context.getStartTimestamp()));
            quartzLog.put(JOB_FINISH_TIME, String.valueOf(context.getEndTimestamp()));
            quartzLog.put(DURATION, String.valueOf(jobExecutionContext.getJobRunTime()));
            quartzLog.put(JOB_EXECUTE_RESULT, null == context.getException() ? JOB_SUCCESS : JOB_FAIL);

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
    public List<Object> collect(IMessageConverter messageConverter,
                                int interval,
                                long timestamp) {
        Queue<Map<String, String>> logs = quartzLogs;
        quartzLogs = new ConcurrentLinkedQueue<>();
        return logs.stream().map(log -> messageConverter.from(log)).collect(Collectors.toList());
    }
}
