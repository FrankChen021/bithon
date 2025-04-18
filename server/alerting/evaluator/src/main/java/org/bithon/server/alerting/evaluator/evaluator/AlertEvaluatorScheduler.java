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

package org.bithon.server.alerting.evaluator.evaluator;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.bithon.server.alerting.common.model.AlertRule;
import org.bithon.server.alerting.evaluator.EvaluatorModuleEnabler;
import org.bithon.server.alerting.evaluator.repository.AlertRepository;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.alerting.pojo.AlertState;
import org.springframework.context.annotation.Conditional;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/11 10:37 上午
 */
@Slf4j
@Service
@EnableScheduling
@Conditional(EvaluatorModuleEnabler.class)
public class AlertEvaluatorScheduler {

    private final AlertEvaluator alertEvaluator;
    private final AlertRepository alertRepository;
    private final ThreadPoolExecutor executor;

    public AlertEvaluatorScheduler(AlertEvaluator alertEvaluator,
                                   AlertRepository alertRepository) {
        this.alertEvaluator = alertEvaluator;
        this.alertRepository = alertRepository;
        this.executor = new ThreadPoolExecutor(1,
                                               Runtime.getRuntime().availableProcessors(),
                                               5,
                                               TimeUnit.MINUTES,
                                               new LinkedBlockingQueue<>(128),
                                               new ThreadFactoryBuilder().setDaemon(true).setNameFormat("alert-evaluator-%d").build(),
                                               new ThreadPoolExecutor.CallerRunsPolicy());
    }

    @Scheduled(cron = "${bithon.alerting.evaluator.scheduler.cron:15 0/1 * 1/1 * ?}")
    public void onSchedule() {
        String name = Thread.currentThread().getName();
        Thread.currentThread().setName("eval-scheduler");
        try {
            // TODO: distributed lock if this server is deployed as multiple instances
            log.info("Starting alert evaluation...");

            // Load changes first
            alertRepository.loadChanges();

            // Load states of all alert rules
            Map<String, AlertState> alertStates = alertRepository.getAlertStates();

            TimeSpan now = TimeSpan.now().floor(Duration.ofMinutes(1));
            for (AlertRule alertRule : alertRepository.getLoadedAlerts().values()) {
                AlertState alertState = alertStates.get(alertRule.getId());
                executor.execute(() -> alertEvaluator.evaluate(now, alertRule, alertState));
            }
        } finally {
            Thread.currentThread().setName(name);
        }
    }
}
