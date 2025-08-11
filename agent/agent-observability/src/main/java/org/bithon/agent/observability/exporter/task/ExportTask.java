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
import org.bithon.component.commons.utils.TimeWindowBasedCounter;

import java.time.Duration;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * @author frankchen
 */
public class ExportTask {

    private static final ILogAdaptor LOG = LoggerFactory.getLogger(ExportTask.class);

    private final Consumer<Object> underlyingSender;
    private final IThreadSafeQueue queue;
    private final ExporterConfig.QueueFullStrategy queueFullStrategy;
    private volatile boolean isRunning = true;
    private volatile boolean isTaskEnded = false;

    /**
     * in millisecond
     */
    private final long flushTime;

    // A counter that will be reset if it's accessed after 5 seconds.
    // We use this to rate-limit the logging of discarded messages.
    private final TimeWindowBasedCounter discardedMessages = new TimeWindowBasedCounter(Duration.ofSeconds(5));

    public ExportTask(String taskName,
                      IThreadSafeQueue queue,
                      ExporterConfig config,
                      Consumer<Object> underlyingSender) {
        this.flushTime = Math.max(10, config.getFlushTime());
        this.underlyingSender = underlyingSender;
        this.queue = config.getBatchSize() > 0 ? new BatchMessageQueue(queue, config.getBatchSize()) : queue;
        this.queueFullStrategy = config.getQueueFullStrategy();
        Thread sendThread = new Thread(() -> {
            while (isRunning) {
                export(true);
            }
            isTaskEnded = true;
        }, taskName + "-sender");
        sendThread.setDaemon(true);
        sendThread.start();
    }

    private void export(boolean waitIfEmpty) {
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
                        LOG.debug("Sending message, size = {}, max batch = {}", size, ((BatchMessageQueue) this.queue).getMaxBatchSize());
                    } else {
                        LOG.debug("Sending message, size = {}", size);
                    }
                }
            }
            this.underlyingSender.accept(message);
        } catch (Exception e) {
            LOG.warn(StringUtils.format("Failed to send message: %s", e.getMessage()), e);
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
        int discarded = 0;
        if (ExporterConfig.QueueFullStrategy.DISCARD_NEWEST.equals(this.queueFullStrategy)) {
            // The return is ignored if the 'offer' fails to run
            if (!this.queue.offer(message)) {
                discarded = 1;
            }
        } else if (ExporterConfig.QueueFullStrategy.DISCARD_OLDEST.equals(this.queueFullStrategy)) {
            // Discard the oldest from the queue
            while (!queue.offer(message)) {
                discarded++;
                queue.pop();
            }
        } else {
            throw new UnsupportedOperationException("Not supported now");
        }

        if (discarded > 0) {
            // Apply rate-limiting to the logging of discarded messages
            long accumulatedCount = discardedMessages.addSync(discarded);
            if (accumulatedCount > 0) {
                LOG.warn("Failed offer element to the queue with a capacity of {}. {} entries are discarded since last report.",
                         this.queue.capacity(),
                         accumulatedCount);
            }
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
            export(false);
        }
    }

    public boolean canAccept() {
        return isRunning;
    }
}
