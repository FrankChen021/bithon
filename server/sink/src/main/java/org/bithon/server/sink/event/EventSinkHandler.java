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

import org.bithon.server.storage.event.EventMessage;
import org.bithon.server.storage.event.IEventWriter;

import java.io.IOException;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/29 21:34
 */
class EventSinkHandler implements EventMessageHandler<EventMessage> {
    private final IEventWriter eventWriter;

    public EventSinkHandler(IEventWriter eventWriter, EventSinkConfig eventSinkConfig) {
        this.eventWriter = new EventBatchWriter(eventWriter, eventSinkConfig);
    }

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
}
