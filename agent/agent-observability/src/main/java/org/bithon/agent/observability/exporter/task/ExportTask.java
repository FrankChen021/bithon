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

    /**
     * Max time that {@link #stop()} waits for the send thread to complete the in-progress send and flush remaining messages.
     * It's longer than the default RPC timeout so that a slow send, like the first one that connects to the server, can still complete.
     */
    private static final long DEFAULT_STOP_TIMEOUT_MS = 10_000;

    private final String exporterName;
    private final Consumer<Object> underlyingSender;
    private final IThreadSafeQueue queue;
    private final ExporterConfig.QueueFullStrategy queueFullStrategy;
    private final Thread sendThread;
    private final long stopTimeoutMs;
    private volatile boolean isRunning = true;

    /**
     * Whether the send thread is waiting for messages. Guarded by 'this'.
     * {@link #stop()} interrupts the send thread only when it's waiting, so that an in-progress send is not interrupted.
     */
    private boolean isWaiting = false;

    /**
     * in millisecond
     */
    private final long flushTime;

    // A counter that will be reset if it's accessed after 5 seconds.
    // We use this to rate-limit the logging of discarded messages.
    private final TimeWindowBasedCounter discardedMessages = new TimeWindowBasedCounter(Duration.ofMinutes(1));

    public ExportTask(String exporterName,
                      IThreadSafeQueue queue,
                      ExporterConfig config,
                      Consumer<Object> underlyingSender) {
        this(exporterName, queue, config, underlyingSender, DEFAULT_STOP_TIMEOUT_MS);
    }

    ExportTask(String exporterName,
               IThreadSafeQueue queue,
               ExporterConfig config,
               Consumer<Object> underlyingSender,
               long stopTimeoutMs) {
        this.exporterName = exporterName;
        this.stopTimeoutMs = stopTimeoutMs;
        this.flushTime = Math.max(10, config.getFlushTime());
        this.underlyingSender = underlyingSender;
        this.queue = config.getBatchSize() > 0 ? new BatchMessageQueue(queue, config.getBatchSize()) : queue;
        this.queueFullStrategy = config.getQueueFullStrategy();
        this.sendThread = new Thread(this::run, exporterName + "-sender");
        this.sendThread.setDaemon(true);
        this.sendThread.start();
    }

    private void run() {
        while (isRunning) {
            export(true);
        }

        // Flush in this thread so that the queue is only consumed by this thread,
        // and stop() does not block on a slow send
        while (queue.size() > 0) {
            export(false);
        }
    }

    private void export(boolean waitIfEmpty) {
        try {
            Object message = waitIfEmpty ? waitAndTake() : queue.take(0);
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
     * Wait for at most {@link #flushTime} to take messages.
     * The wait is interrupted by {@link #stop()} so that the shutdown does not need to wait for the flush time.
     */
    private Object waitAndTake() {
        synchronized (this) {
            if (!isRunning) {
                // The remaining messages are flushed after the loop
                return null;
            }
            isWaiting = true;
        }
        try {
            return queue.take(this.flushTime);
        } catch (InterruptedException ignored) {
            return null;
        } finally {
            synchronized (this) {
                isWaiting = false;

                // Clear the interrupt status that might be set by 'stop'
                // so that it does not affect the sending of the messages taken above
                Thread.interrupted();
            }
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
            long accumulatedCount = discardedMessages.synchronousAdd(discarded);
            if (accumulatedCount > 0) {
                LOG.warn("Failed to offer element to the {} queue(capacity={}). {} entries have been discarded since last report.",
                         this.exporterName,
                         this.queue.capacity(),
                         accumulatedCount);
            }
        }
    }

    public void stop() {
        // stop receiving new messages and stop the task
        synchronized (this) {
            isRunning = false;

            // Wake up the send thread if it's waiting for messages
            if (isWaiting) {
                sendThread.interrupt();
            }
        }

        // Wait for the send thread to complete sending and flushing
        try {
            sendThread.join(stopTimeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (sendThread.isAlive()) {
            // The send thread is a daemon thread, it does not block the JVM from exiting
            LOG.warn("The send thread of exporter [{}] does not complete in {} ms. Remaining messages might be discarded.",
                     exporterName,
                     stopTimeoutMs);
        }
    }

    public boolean canAccept() {
        return isRunning;
    }
}
