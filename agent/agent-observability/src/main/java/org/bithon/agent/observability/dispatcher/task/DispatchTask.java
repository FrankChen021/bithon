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

package org.bithon.agent.observability.dispatcher.task;

import org.bithon.agent.observability.dispatcher.config.DispatcherConfig;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.StringUtils;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * @author frankchen
 */
public class DispatchTask {

    private static final ILogAdaptor log = LoggerFactory.getLogger(DispatchTask.class);

    private final Consumer<Object> underlyingSender;
    private final IMessageQueue queue;
    private volatile boolean isRunning = true;
    private volatile boolean isTaskEnd = false;

    /**
     * in millisecond
     */
    private final long flushTime;
    private final int batchSize;

    public DispatchTask(String taskName,
                        IMessageQueue queue,
                        DispatcherConfig config,
                        Consumer<Object> underlyingSender) {
        this.flushTime = Math.max(10, config.getFlushTime());
        this.batchSize = Math.max(1, config.getBatchSize());
        this.underlyingSender = underlyingSender;
        this.queue = queue;
        new Thread(() -> {
            while (isRunning) {
                dispatch(true);
            }
            isTaskEnd = true;
        }, taskName + "-sender").start();
    }

    private void dispatch(boolean wait) {
        try {
            Object message = wait ? queue.take(this.batchSize, this.flushTime) : queue.take(this.batchSize);
            if (message != null) {
                if (batchSize > 1 && message instanceof Collection) {
                    int size = ((Collection) message).size();
                    log.info("Sending message in batch, size = {}", size);
                }
                this.underlyingSender.accept(message);
            }
        } catch (Exception e) {
            log.error(StringUtils.format("Failed to send message: %s", e.getMessage()), e);
        }
    }

    public void accept(Object message) {
        if (log.isDebugEnabled()) {
            String className = message.getClass().getSimpleName();
            className = className.replace("Entity", "");
            log.debug("Entity : " + className + ", Got and Send : " + message);
        }
        if (isRunning) {
            queue.offer(message);
        }
    }

    public void accept(Collection<Object> messages) {
        if (isRunning) {
            queue.offerAll(messages);
        }
    }

    public void stop() {
        // stop receiving new messages and stop the task
        isRunning = false;

        while (!isTaskEnd) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }

        // flush all messages
        while (queue.size() > 0) {
            dispatch(false);
        }
    }

    public boolean canAccept() {
        return isRunning;
    }
}
