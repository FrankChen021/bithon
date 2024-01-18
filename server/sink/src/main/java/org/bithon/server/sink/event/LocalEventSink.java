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

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.OptBoolean;
import org.bithon.component.commons.collection.IteratorableCollection;
import org.bithon.server.sink.common.handler.AbstractThreadPoolMessageHandler;
import org.bithon.server.storage.event.EventMessage;

import java.time.Duration;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/16
 */
@JsonTypeName("local")
public class LocalEventSink implements IEventMessageSink {

    static class EventMessageProcessor extends AbstractThreadPoolMessageHandler<IteratorableCollection<EventMessage>> {

        private final EventMessageHandlers handlers;

        public EventMessageProcessor(EventMessageHandlers handlers) {
            super("event-message-handler", 1, 5, Duration.ofMinutes(3), 1024, new ThreadPoolExecutor.DiscardOldestPolicy());
            this.handlers = handlers;
        }

        @Override
        protected void onMessage(IteratorableCollection<EventMessage> iterator) {
            handlers.handle(iterator);
        }

        @Override
        public String getType() {
            return "event";
        }
    }

    private final EventMessageProcessor messageProcessor;

    @JsonCreator
    public LocalEventSink(@JacksonInject(useInput = OptBoolean.FALSE) EventMessageHandlers handlers) {
        this.messageProcessor = new EventMessageProcessor(handlers);
    }

    @Override
    public void process(String messageType, IteratorableCollection<EventMessage> message) {
        this.messageProcessor.submit(message);
    }

    @Override
    public void close() throws Exception {
        messageProcessor.close();
    }
}
