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
import org.bithon.component.commons.collection.IteratorableCollection;
import org.bithon.server.storage.event.EventMessage;
import org.bithon.server.storage.event.IEventStorage;
import org.bithon.server.storage.event.IEventWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/8/2 22:10
 */
@Slf4j
public class EventMessageHandlers {

    private final Map<String, EventMessageHandler<?>> handlers = new ConcurrentHashMap<>(7);

    private final EventMessageHandler<?> defaultHandler;

    public EventMessageHandlers(IEventStorage eventStorage) {
        defaultHandler = new EventMessageHandler<EventMessage>() {
            final IEventWriter eventWriter = eventStorage.createWriter();

            @Override
            public String getEventType() {
                return "all";
            }

            @Override
            public EventMessage transform(EventMessage eventMessage) {
                return eventMessage;
            }

            @Override
            public void process(List<EventMessage> messages) throws IOException {
                if (messages.isEmpty()) {
                    return;
                }
                eventWriter.write(messages);
            }
        };
    }

    static class PiplelineHandler<T> {
        private final EventMessageHandler<T> delegation;
        private final List<T> messages = new ArrayList<>();

        public PiplelineHandler(EventMessageHandler<T> delegation) {
            this.delegation = delegation;
        }

        public void transform(EventMessage eventMessage) {
            try {
                T message = delegation.transform(eventMessage);
                if (message != null) {
                    messages.add(message);
                }
            } catch (IOException e) {
                log.warn("Exception when transform event message[{}]: {}", eventMessage, e.getMessage());
            }
        }

        public void process() throws IOException {
            if (!messages.isEmpty()) {
                delegation.process(messages);
            }
        }
    }

    public void handle(IteratorableCollection<EventMessage> iterator) {
        Map<String, PiplelineHandler<?>> pipelineHandlers = new HashMap<>(handlers.size() + 1);

        // phase 1, add event message for each handler
        while (iterator.hasNext()) {
            EventMessage message = iterator.next();

            String eventType = message.getType();
            PiplelineHandler<?> handler = pipelineHandlers.computeIfAbsent(eventType,
                                                                           v -> new PiplelineHandler<>(handlers.getOrDefault(eventType,
                                                                                                                             defaultHandler)));
            handler.transform(message);
        }

        // phase 2, process messages for each handler in batch
        pipelineHandlers.forEach((eventType, handler) -> {
            try {
                handler.process();
            } catch (Exception e) {
                log.error("Error to process event[{}]: {}", eventType, e.getMessage());
            }
        });
    }

    public void add(EventMessageHandler<?> handler) {
        handlers.put(handler.getEventType(), handler);
    }

    public void remove(String eventType) {
        handlers.remove(eventType);
    }
}
