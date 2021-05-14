/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.core.dispatcher.task;

import com.sbss.bithon.agent.core.config.DispatcherConfig;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author frankchen
 */
public class DispatchTask {

    private static final Logger log = LoggerFactory.getLogger(DispatchTask.class);

    private final Consumer<Object> messageConsumer;
    private final long gcPeriod;
    private final IMessageQueue queue;
    private final String taskName;
    private long lastGCTime;

    public DispatchTask(String taskName,
                        IMessageQueue queue,
                        DispatcherConfig config,
                        Consumer<Object> messageConsumer) {
        this.taskName = taskName;
        this.gcPeriod = config.getQueue().getGcPeriod() * 1000L;
        this.messageConsumer = messageConsumer;
        this.queue = queue;
        this.lastGCTime = System.currentTimeMillis();

        new Thread(this::send, taskName + "-sender").start();
    }

    private void gcIfNeed() {
        long sysTime = System.currentTimeMillis();
        if (sysTime - lastGCTime >= this.gcPeriod) {
            queue.gc();
            lastGCTime = sysTime;
            log.debug("GC for {}, current size: {}", taskName, queue.size());
        }
    }

    private void send() {
        while (true) {
            try {
                Object message = queue.dequeue(10, TimeUnit.MILLISECONDS);
                if (message != null) {
                    this.messageConsumer.accept(message);
                    gcIfNeed();
                }
            } catch (Exception e) {
                log.error("Sending Entity Failed! \n" + e, e);
            }
        }
    }

    public void sendMessage(Object message) {
        if (log.isDebugEnabled()) {
            String className = message.getClass().getSimpleName();
            className = className.replace("Entity", "");
            log.debug("Entity : " + className + ", Got and Send : " + message.toString());
        }
        queue.enqueue(message);
    }
}
