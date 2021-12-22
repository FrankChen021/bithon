package org.bithon.server.web.service.event.api;

import org.bithon.server.common.utils.datetime.TimeSpan;
import org.bithon.server.event.storage.Event;
import org.bithon.server.event.storage.IEventReader;
import org.bithon.server.event.storage.IEventStorage;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author Frank Chen
 * @date 22/12/21 11:17 AM
 */
@RestController
public class EventApi implements IEventApi {

    private final IEventReader eventReader;

    public EventApi(IEventStorage eventStorage) {
        this.eventReader = eventStorage.createReader();
    }

    @Override
    public GetEventListResponse getEventList(GetEventListRequest request) {
        TimeSpan start = TimeSpan.fromISO8601(request.getStartTimeISO8601());
        TimeSpan end = TimeSpan.fromISO8601(request.getEndTimeISO8601());

        List<Event> events = eventReader.getEventList(request.getApplication(), start, end);
        return new GetEventListResponse(events.size(), events);
    }
}
