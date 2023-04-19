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
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.StringUtils;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author frankchen
 */
public class DispatchTask {

    private static final ILogAdaptor LOG = LoggerFactory.getLogger(DispatchTask.class);

    private final Consumer<Object> underlyingSender;
    private final IMessageQueue bufferQueue;
    private volatile boolean isRunning = true;
    private volatile boolean isTaskEnd = false;

    /**
     * in millisecond
     */
    private final long flushTime;

    private final ThreadPoolExecutor retryHandler;

    public DispatchTask(String taskName,
                        IMessageQueue bufferQueue,
                        DispatcherConfig config,
                        Consumer<Object> underlyingSender) {
        this.flushTime = Math.max(10, config.getFlushTime());
        this.underlyingSender = underlyingSender;
        this.bufferQueue = config.getBatchSize() > 0 ? new BatchMessageQueue(bufferQueue, config.getBatchSize()) : bufferQueue;
        new Thread(() -> {
            while (isRunning) {
                dispatch(true);
            }
            isTaskEnd = true;
        }, taskName + "-sender").start();

        this.retryHandler = new ThreadPoolExecutor(0,
                                                   4,
                                                   60L,
                                                   TimeUnit.SECONDS,
                                                   new LinkedBlockingQueue<>(32),
                                                   NamedThreadFactory.of(taskName + "-queue"),
                                                   new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    private void dispatch(boolean waitIfEmpty) {
        try {
            Object message = bufferQueue.take(waitIfEmpty ? this.flushTime : 0);
            if (message == null) {
                return;
            }

            if (message instanceof Collection) {
                int size = ((Collection<?>) message).size();
                if (size == 0) {
                    return;
                }

                LOG.info("Sending message, size = {}, batch = {}",
                         size,
                         this.bufferQueue instanceof BatchMessageQueue ? ((BatchMessageQueue) this.bufferQueue).getBatchSize() : -1);
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

        if (!(message instanceof List) && bufferQueue instanceof BatchMessageQueue) {
            bufferQueue.offer(Collections.singletonList(message));
        } else {
            bufferQueue.offer(message);
        }
    }

    public void accept(List<Object> messages) {
        if (!isRunning) {
            return;
        }

        // Since this accept method is not atomic, when the code goes here and below 'stop' is called and runs to complete,
        // the message will be still added to the queue.
        //
        // To solve the problem, it requires some lock mechanism between this method and below 'stop' method,
        // but because the underlying queue is already a concurrency supported structure,
        // adding such lock to solve this edge case does not gain much
        //
        if (!bufferQueue.offer(messages)) {
            retryHandler.execute(() -> {
                try {
                    bufferQueue.offer(messages, Duration.ofMillis(50));
                } catch (InterruptedException ignored) {
                }
            });
        }
    }

    public void stop() {
        // stop receiving new messages and stop the task
        isRunning = false;

        // Wait for send task to complete
        while (!isTaskEnd) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }

        // Wait for thread pool to complete
        this.retryHandler.shutdown();
        try {
            this.retryHandler.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }

        // flush all messages
        while (bufferQueue.size() > 0) {
            dispatch(false);
        }
    }

    public boolean canAccept() {
        return isRunning;
    }
}
