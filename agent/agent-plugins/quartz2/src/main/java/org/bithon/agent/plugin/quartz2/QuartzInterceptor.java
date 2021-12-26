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

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.core.dispatcher.Dispatcher;
import org.bithon.agent.core.dispatcher.Dispatchers;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.quartz.impl.SchedulerRepository;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author frankchen
 */
public class QuartzInterceptor extends AbstractInterceptor {
    private static final ILogAdaptor log = LoggerFactory.getLogger(QuartzInterceptor.class);

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

    /*
    private QuartzEntity buildJobCountsEntity() throws SchedulerException {
        QuartzEntity q = quartzMonitor.buildQuartzData();

        q.setAppName(dispatcher.getAppName());
        q.setHostName(dispatcher.getIpAddress());
        q.setPort(dispatcher.getPort());
        q.setCollectCount(1);
        q.setTimestamp(System.currentTimeMillis());
        q.setInterval(checkPeriod);

        return q;
    }

    private QuartzInfoEntity buildJobListEntity() throws SchedulerException {
        List<JobTriggerEntity> jobTriggerEntityList = quartzMonitor.buildQuartJobList();

        QuartzInfoEntity quartzInfoEntity = new QuartzInfoEntity();

        quartzInfoEntity.setAppName(dispatcher.getAppName());
        quartzInfoEntity.setHostName(dispatcher.getIpAddress());
        quartzInfoEntity.setPort(dispatcher.getPort());
        quartzInfoEntity.setJobTriggerList(jobTriggerEntityList);

        return quartzInfoEntity;
    }*/

    private void dispatch() {
        try {
            if (dispatcher.isReady()) {
                /*
                dispatcher.sendMetrics(buildJobCountsEntity());
                QuartzInfoEntity quartzInfoEntity = buildJobListEntity();
                if (!quartzInfoEntity.getJobTriggerList().isEmpty()) {
                    dispatcher.sendMetrics(quartzInfoEntity);
                }
                */
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
