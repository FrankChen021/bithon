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
import org.bithon.server.sink.common.FixedDelayExecutor;
import org.bithon.server.storage.event.EventMessage;
import org.bithon.server.storage.event.IEventWriter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/29 21:09
 */
@Slf4j
public class EventBatchWriter implements IEventWriter {

    private final IEventWriter delegation;
    private final FixedDelayExecutor executor;
    private final EventSinkConfig sinkConfig;
    private List<EventMessage> events;

    public EventBatchWriter(IEventWriter delegation, EventSinkConfig sinkConfig) {
        this.delegation = delegation;
        this.sinkConfig = sinkConfig;
        this.events = new ArrayList<>(getBatchSize());
        this.executor = new FixedDelayExecutor("event-batch-writer",
                                               this::flush,
                                               5,
                                               () -> Duration.ofSeconds(sinkConfig.getBatch() == null ? 1 : sinkConfig.getBatch().getInterval()));
    }

    @Override
    public void write(List<EventMessage> eventMessage) {
        synchronized (this) {
            this.events.addAll(eventMessage);
        }
        if (this.events.size() > getBatchSize()) {
            flush();
        }
    }

    @Override
    public void close() throws Exception {
        log.info("Shutting down event batch writer...");

        // shutdown and wait for current scheduler to close
        try {
            this.executor.shutdown(Duration.ofSeconds(20));
        } catch (InterruptedException ignored) {
        }

        // flush all data to see if there's any more data
        flush();

        // close underlying writer at last
        this.delegation.close();
    }

    /**
     * Get the size from config so that the 'size' can be dynamically in effect if it's changed in configuration center such as nacos/apollo
     */
    private int getBatchSize() {
        return sinkConfig.getBatch() == null ? 2000 : sinkConfig.getBatch().getSize();
    }

    private void flush() {
        if (this.events.isEmpty()) {
            return;
        }

        // Swap the object for flushing
        List<EventMessage> flushEvents;
        synchronized (this) {
            flushEvents = this.events;

            this.events = new ArrayList<>(getBatchSize());
        }

        // Double check
        if (flushEvents.isEmpty()) {
            return;
        }
        try {
            log.debug("Flushing [{}] events into storage...", flushEvents.size());
            this.delegation.write(flushEvents);
        } catch (Exception e) {
            log.error("Exception when flushing events into storage", e);
        }
    }
}
