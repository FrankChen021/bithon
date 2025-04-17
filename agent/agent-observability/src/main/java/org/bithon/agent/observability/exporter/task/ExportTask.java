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

package org.bithon.agent.observability.exporter.task;

import org.bithon.agent.observability.exporter.config.ExporterConfig;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.StringUtils;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * @author frankchen
 */
public class ExportTask {

    private static final ILogAdaptor LOG = LoggerFactory.getLogger(ExportTask.class);

    private final Consumer<Object> underlyingSender;
    private final IMessageQueue queue;
    private final ExporterConfig.QueueFullStrategy queueFullStrategy;
    private volatile boolean isRunning = true;
    private volatile boolean isTaskEnd = false;

    /**
     * in millisecond
     */
    private final long flushTime;

    public ExportTask(String taskName,
                      IMessageQueue queue,
                      ExporterConfig config,
                      Consumer<Object> underlyingSender) {
        this.flushTime = Math.max(10, config.getFlushTime());
        this.underlyingSender = underlyingSender;
        this.queue = config.getBatchSize() > 0 ? new BatchMessageQueue(queue, config.getBatchSize()) : queue;
        this.queueFullStrategy = config.getQueueFullStrategy();
        Thread sendThread = new Thread(() -> {
            while (isRunning) {
                dispatch(true);
            }
            isTaskEnd = true;
        }, taskName + "-sender");
        sendThread.setDaemon(true);
        sendThread.start();
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
                        LOG.debug("Sending message, size = {}, max batch = {}", size, ((BatchMessageQueue) this.queue).getBatchSize());
                    } else {
                        LOG.debug("Sending message, size = {}", size);
                    }
                }
            }
            this.underlyingSender.accept(message);
        } catch (Exception e) {
            LOG.error(StringUtils.format("Failed to send message: %s", e.getMessage()), e);
        }
    }

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
        if (ExporterConfig.QueueFullStrategy.DISCARD.equals(this.queueFullStrategy)) {
            // The return is ignored if the 'offer' fails to run
            this.queue.offer(message);
        } else if (ExporterConfig.QueueFullStrategy.DISCARD_OLDEST.equals(this.queueFullStrategy)) {
            // Discard the oldest in the queue
            while (!queue.offer(message)) {
                LOG.error("Failed offer element to the queue, capacity = {}. Discarding the oldest...", this.queue.capacity());
                queue.pop();
            }
        } else {
            throw new UnsupportedOperationException("Not supported now");
        }
    }

    public void stop() {
        // stop receiving new messages and stop the task
        isRunning = false;

        // Wait for the send task to complete
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
