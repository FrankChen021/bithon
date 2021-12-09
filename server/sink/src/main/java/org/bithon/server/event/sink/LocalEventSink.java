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

package org.bithon.server.event.sink;

import org.bithon.server.common.utils.collection.CloseableIterator;
import org.springframework.context.ApplicationContext;

import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/16
 */
public class LocalEventSink implements IEventMessageSink {

    private final EventsMessageHandler handler;

    public LocalEventSink(ApplicationContext applicationContext) throws IOException {
        this.handler = new EventsMessageHandler(applicationContext);
    }

    @Override
    public void process(String messageType, CloseableIterator<EventMessage> message) {
        this.handler.submit(message);
    }
}
