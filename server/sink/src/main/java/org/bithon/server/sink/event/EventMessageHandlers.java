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
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frank.chen
 * @date 2022/8/2 22:10
 */
@Slf4j
@Component
public class EventMessageHandlers {

    private final Map<String, EventMessageHandler> handlers = new ConcurrentHashMap<>(7);

    private final EventMessageHandler defaultHandler;

    public EventMessageHandlers(IEventStorage eventStorage) {
        defaultHandler = new EventMessageHandler() {
            final IEventWriter eventWriter = eventStorage.createWriter();

            List<EventMessage> batchMessages;

            @Override
            public String getEventType() {
                return "all";
            }

            @Override
            public void startProcessing() {
                batchMessages = new ArrayList<>();
            }

            @Override
            public void transform(EventMessage eventMessage) {
                batchMessages.add(eventMessage);
            }

            @Override
            public void finalizeProcessing() throws IOException {
                if (batchMessages.isEmpty()) {
                    return;
                }
                try {
                    eventWriter.write(batchMessages);
                } finally {
                    // deference this object
                    batchMessages = null;
                }
            }
        };
    }

    public void handle(IteratorableCollection<EventMessage> iterator) {
        // phase 1, process messages for each handler in batch
        for (EventMessageHandler handler : handlers.values()) {
            try {
                handler.startProcessing();
            } catch (Exception ignored) {
            }
        }

        // phase 2, add event message for each handler
        while (iterator.hasNext()) {
            EventMessage message = iterator.next();

            EventMessageHandler handler = handlers.getOrDefault(message.getType(), defaultHandler);
            handler.transform(message);
        }

        // phase 3, process messages for each handler in batch
        for (EventMessageHandler handler : handlers.values()) {
            try {
                handler.finalizeProcessing();
            } catch (Exception e) {
                log.error("Error to process event[{}]: {}", handler.getEventType(), e.getMessage());
            }
        }
    }

    public void add(EventMessageHandler handler) {
        handlers.put(handler.getEventType(), handler);
    }

    public void remove(String eventType) {
        handlers.remove(eventType);
    }
}
