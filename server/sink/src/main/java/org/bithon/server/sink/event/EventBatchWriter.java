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

package org.bithon.server.sink.event;

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.server.storage.event.EventMessage;
import org.bithon.server.storage.event.IEventWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/29 21:09
 */
@Slf4j
public class EventBatchWriter implements IEventWriter {

    private final IEventWriter delegation;
    private final ScheduledExecutorService executor;
    private final int batchSize;
    private List<EventMessage> events;

    public EventBatchWriter(IEventWriter delegation, int batchSize, int interval) {
        this.delegation = delegation;
        this.events = new ArrayList<>(batchSize);
        this.batchSize = batchSize;
        this.executor = Executors.newSingleThreadScheduledExecutor(NamedThreadFactory.of("event-batch-writer"));
        this.executor.scheduleWithFixedDelay(this::flush, 5, interval, TimeUnit.SECONDS);
    }

    @Override
    public void write(List<EventMessage> eventMessage) {
        synchronized (this) {
            this.events.addAll(eventMessage);
        }
        if (events.size() > this.batchSize) {
            flush();
        }
    }

    @Override
    public void close() throws Exception {
        log.info("Shutting down event batch writer...");

        // shutdown and wait for current scheduler to close
        this.executor.shutdown();
        try {
            if (!this.executor.awaitTermination(20, TimeUnit.SECONDS)) {
                log.warn("Timeout when shutdown trace batch writer");
            }
        } catch (InterruptedException ignored) {
        }

        // flush all data to see if there's any more data
        flush();

        // close underlying writer at last
        this.delegation.close();
    }

    private void flush() {
        if (this.events.isEmpty()) {
            return;
        }

        // Swap the object for flushing
        List<EventMessage> flushEvents;
        synchronized (this) {
            flushEvents = this.events;

            this.events = new ArrayList<>(this.batchSize);
        }

        // Double check
        if (flushEvents.isEmpty()) {
            return;
        }
        try {
            log.debug("Flushing [{}] events into storage...", flushEvents.size());
            this.delegation.write(events);
        } catch (Exception e) {
            log.error("Exception when flushing spans into storage", e);
        }
    }
}
