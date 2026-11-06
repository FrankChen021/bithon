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
     * Max time to wait for the send thread to complete the in-progress send and flush remaining messages,
     * used by {@link #stop()}, and as the shared deadline of all exporters when the agent shuts down.
     * It's longer than the default RPC timeout so that a slow in-progress send can still complete.
     */
    public static final long DEFAULT_STOP_TIMEOUT_MS = 10_000;

    private final String exporterName;
    private final Consumer<Object> underlyingSender;
    private final IThreadSafeQueue queue;
    private final ExporterConfig.QueueFullStrategy queueFullStrategy;
    private final Thread sendThread;
    private final long stopTimeoutMs;
    private final StopSignal stopSignal = new StopSignal();

    /**
     * Set when the stop times out, so that the send thread discards the remaining messages instead of sending them.
     * Otherwise, the send thread keeps sending them after the underlying exporter is closed, and logs an error for each of them.
     */
    private volatile boolean isDiscarding = false;

    /**
     * in millisecond
     */
    private final long flushTime;

    // A counter that will be reset if it's accessed after 1 minute.
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
        this.sendThread = new Thread(
            () -> {
                while (!stopSignal.isStopped()) {
                    send(true);
                }

                // Flush in this thread so that the queue is only consumed by this thread,
                // and stop() does not block on a slow send
                while (!isDiscarding && this.queue.size() > 0) {
                    send(false);
                }
            }, exporterName + "-sender");
        this.sendThread.setDaemon(true);
        this.sendThread.start();
    }

    public boolean canExport() {
        return !stopSignal.isStopped();
    }

    private void send(boolean waitIfEmpty) {
        try {
            Object message = waitIfEmpty ? stopSignal.waitUnlessStopped(() -> queue.take(this.flushTime)) : queue.take(0);
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
    public void export(Object message) {
        if (stopSignal.isStopped()) {
            return;
        }

        // Since this 'export' method is not atomic, when the code goes here and below 'stop' is called and runs to complete,
        // the message will be still added to the queue, and it might not be flushed.
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
        long deadline = System.currentTimeMillis() + stopTimeoutMs;
        signalStop();
        awaitTermination(deadline);
    }

    /**
     * Stop receiving new messages, and wake up the send thread if it's waiting for messages.
     * The send thread then sends the remaining messages and exits.
     * <p>
     * Call it on all tasks before calling {@link #awaitTermination(long)} so that they flush in parallel.
     */
    public void signalStop() {
        stopSignal.stop();
    }

    /**
     * Wait for the send thread to send the remaining messages until the given deadline.
     * If the send thread does not complete before the deadline, the remaining messages are discarded.
     *
     * @param deadline in milliseconds since epoch
     */
    public void awaitTermination(long deadline) {
        if (Thread.currentThread() == sendThread) {
            // Joining itself would block until the deadline
            return;
        }

        // If the caller is interrupted, keep waiting until the deadline, and restore the interrupt status after the wait
        boolean interrupted = false;
        long remaining;
        while (sendThread.isAlive() && (remaining = deadline - System.currentTimeMillis()) > 0) {
            try {
                sendThread.join(remaining);
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        if (sendThread.isAlive()) {
            isDiscarding = true;

            // The send thread is a daemon thread, it does not block the JVM from exiting
            LOG.warn("The send thread of exporter [{}] does not complete before the deadline. Remaining messages are discarded.",
                     exporterName);
        }
    }
}
