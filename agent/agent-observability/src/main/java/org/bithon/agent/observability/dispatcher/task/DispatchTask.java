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

    private static final ILogAdaptor LOG = LoggerFactory.getLogger(DispatchTask.class);

    private final Consumer<Object> underlyingSender;
    private final IThreadSafeQueue queue;
    private final DispatcherConfig.QueueFullStrategy queueFullStrategy;
    private volatile boolean isRunning = true;
    private volatile boolean isTaskEnded = false;

    /**
     * in millisecond
     */
    private final long flushTime;

    public DispatchTask(String taskName,
                        IThreadSafeQueue queue,
                        DispatcherConfig config,
                        Consumer<Object> underlyingSender) {
        this.flushTime = Math.max(10, config.getFlushTime());
        this.underlyingSender = underlyingSender;
        this.queue = config.getBatchSize() > 0 ? new BatchMessageQueue(queue, config.getBatchSize()) : queue;
        this.queueFullStrategy = config.getQueueFullStrategy();
        new Thread(() -> {
            while (isRunning) {
                dispatch(true);
            }
            isTaskEnded = true;
        }, taskName + "-sender").start();
    }

    private void dispatch(boolean waitIfEmpty) {
        try {
            Object message = queue.take(waitIfEmpty ? this.flushTime : 0);
            if (message == null) {
                return;
            }

            if (message instanceof Collection) {
                int size = ((Collection<?>) message).size();
                if (size == 0) {
                    return;
                }

                if (LOG.isDebugEnabled()) {
                    if (queue instanceof BatchMessageQueue) {
                        LOG.info("Sending message, size = {}, max batch = {}", size, ((BatchMessageQueue) this.queue).getBatchSize());
                    } else {
                        LOG.info("Sending message, size = {}", size);
                    }
                }
            }
            this.underlyingSender.accept(message);
        } catch (Exception e) {
            LOG.error(StringUtils.format("Failed to send message: %s", e.getMessage()), e);
        }
    }

    /**
     * User code might call this public method in multiple threads, thread-safe must be guaranteed.
     */
    public void accept(Object message) {
        if (!isRunning) {
            return;
        }

        // Since this 'accept' method is not atomic, when the code goes here and below 'stop' is called and runs to complete,
        // the message will be still added to the queue.
        //
        // To solve the problem, it requires some lock mechanism between this method and below 'stop' method,
        // but because the underlying queue is already a concurrency-supported structure,
        // adding such a lock to solve this edge case does not gain much
        //
        if (DispatcherConfig.QueueFullStrategy.DISCARD_NEWEST.equals(this.queueFullStrategy)) {
            // The 'message' will be discarded if the queue is full
            this.queue.offer(message);
        } else if (DispatcherConfig.QueueFullStrategy.DISCARD_OLDEST.equals(this.queueFullStrategy)) {
            // Discard the oldest in the queue
            int discarded = 0;
            while (!queue.offer(message)) {
                discarded++;
                queue.pop();
            }
            if (discarded > 0) {
                LOG.error("Failed offer element to the queue, capacity = {}. Discarded the {} oldest entry", this.queue.capacity(), discarded);
            }
        } else {
            throw new UnsupportedOperationException("Not supported now");
        }
    }

    public void stop() {
        // stop receiving new messages and stop the task
        isRunning = false;

        // Wait for the send task to complete
        while (!isTaskEnded) {
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
