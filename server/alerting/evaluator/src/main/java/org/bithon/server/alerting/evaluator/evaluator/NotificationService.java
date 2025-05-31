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


import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.server.alerting.common.evaluator.EvaluationLogger;
import org.bithon.server.alerting.notification.api.INotificationApiStub;
import org.bithon.server.alerting.notification.message.NotificationMessage;
import org.bithon.server.storage.alerting.IEvaluationLogStorage;
import org.bithon.server.storage.alerting.IEvaluationLogWriter;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 31/5/25 4:04 pm
 */
public class NotificationService implements INotificationApiStub {
    private final INotificationApiStub impl;
    private final IEvaluationLogStorage logStorage;

    // Cached thread pool
    private final ThreadPoolExecutor notificationThreadPool = new ThreadPoolExecutor(1,
                                                                                     10,
                                                                                     3,
                                                                                     TimeUnit.MINUTES,
                                                                                     new SynchronousQueue<>(),
                                                                                     NamedThreadFactory.nonDaemonThreadFactory("notification"),
                                                                                     new ThreadPoolExecutor.CallerRunsPolicy());

    public NotificationService(INotificationApiStub impl,
                               IEvaluationLogStorage logStorage) {
        this.impl = impl;
        this.logStorage = logStorage;
    }

    @Override
    public void notify(String name, NotificationMessage message) {
        notificationThreadPool.execute(() -> {
            try {
                impl.notify(name, message);
            } catch (Exception e) {
                try (IEvaluationLogWriter writer = logStorage.createWriter()) {
                    new EvaluationLogger(writer).error(message.getAlertRule().getId(),
                                                       message.getAlertRule().getName(),
                                                       AlertEvaluator.class,
                                                       e,
                                                       "Failed to send notification to channel [%s]",
                                                       name);
                }
            }
        });
    }
}
