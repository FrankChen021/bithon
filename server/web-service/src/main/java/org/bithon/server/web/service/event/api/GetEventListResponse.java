package org.bithon.server.web.service.event.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bithon.server.event.storage.Event;

import java.util.List;

/**
 * @author Frank Chen
 * @date 22/12/21 11:19 AM
 */
@Data
@AllArgsConstructor
public class GetEventListResponse {
    private int total;
    private List<Event> rows;
}
