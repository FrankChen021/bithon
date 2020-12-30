package com.sbss.bithon.collector.events.message;

import com.sbss.bithon.collector.common.message.handlers.AbstractThreadPoolMessageHandler;
import com.sbss.bithon.collector.events.storage.IEventStorage;
import com.sbss.bithon.collector.events.storage.IEventWriter;
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
        super(1, 5, Duration.ofMinutes(3), 1024);
        eventWriter = eventStorage.createWriter();
    }

    @Override
    protected void onMessage(EventMessage message) throws IOException {
        log.info("Receiving Event Message: {}", message);
        eventWriter.write(message);
    }
}
