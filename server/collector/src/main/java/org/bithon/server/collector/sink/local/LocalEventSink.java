/*
 *    Copyright 2020 bithon.cn
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

package org.bithon.server.collector.sink.local;

import org.bithon.server.collector.sink.IMessageSink;
import org.bithon.server.event.handler.EventMessage;
import org.bithon.server.event.handler.EventsMessageHandler;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/16
 */
public class LocalEventSink implements IMessageSink<EventMessage> {

    private final EventsMessageHandler handler;

    public LocalEventSink(EventsMessageHandler handler) {
        this.handler = handler;
    }

    @Override
    public void process(String messageType, EventMessage message) {
        this.handler.submit(message);
    }
}
