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

package com.sbss.bithon.server.event.handler;

import com.sbss.bithon.server.common.handler.AbstractThreadPoolMessageHandler;
import com.sbss.bithon.server.event.storage.IEventStorage;
import com.sbss.bithon.server.event.storage.IEventWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 4:01 下午
 */
@Slf4j
@Component
public class EventsMessageHandler extends AbstractThreadPoolMessageHandler<EventMessage> {

    final IEventWriter eventWriter;

    public EventsMessageHandler(IEventStorage eventStorage) {
        super("event", 1, 5, Duration.ofMinutes(3), 1024);
        eventWriter = eventStorage.createWriter();
    }

    @Override
    protected void onMessage(EventMessage body) throws IOException {
        log.info("Receiving Event Message: {}", body);
        eventWriter.write(body);
    }

    @Override
    public String getType() {
        return "event";
    }
}
