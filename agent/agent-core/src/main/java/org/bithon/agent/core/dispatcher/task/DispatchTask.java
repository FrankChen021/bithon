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

package org.bithon.agent.core.dispatcher.task;

import org.bithon.agent.core.dispatcher.config.DispatcherConfig;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author frankchen
 */
public class DispatchTask {

    private static final ILogAdaptor log = LoggerFactory.getLogger(DispatchTask.class);

    private final Consumer<Object> messageConsumer;
    private final long gcPeriod;
    private final IMessageQueue queue;
    private final String taskName;
    private long lastGCTime;
    private boolean isRunning = true;

    public DispatchTask(String taskName,
                        IMessageQueue queue,
                        DispatcherConfig config,
                        Consumer<Object> messageConsumer) {
        this.taskName = taskName;
        this.gcPeriod = config.getQueue().getGcPeriod() * 1000L;
        this.messageConsumer = messageConsumer;
        this.queue = queue;
        this.lastGCTime = System.currentTimeMillis();

        new Thread(() -> {
            while (isRunning) {
                send();
            }
        }, taskName + "-sender").start();
    }

    private void send() {
        try {
            Object message = queue.dequeue(10, TimeUnit.MILLISECONDS);
            if (message != null) {
                this.messageConsumer.accept(message);
                queue.gc();
            }
        } catch (Exception e) {
            log.error("Sending Entity Failed! \n" + e, e);
        }
    }

    public void accept(Object message) {
        if (log.isDebugEnabled()) {
            String className = message.getClass().getSimpleName();
            className = className.replace("Entity", "");
            log.debug("Entity : " + className + ", Got and Send : " + message);
        }
        if (isRunning) {
            queue.enqueue(message);
        }
    }

    public void stop() {
        // stop receiving new messages
        isRunning = false;

        // flush all messages
        while (queue.size() > 0) {
            send();
        }
    }

    public boolean canAccept() {
        return isRunning;
    }
}
