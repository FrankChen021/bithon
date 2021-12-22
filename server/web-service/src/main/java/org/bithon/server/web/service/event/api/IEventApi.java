package org.bithon.server.web.service.event.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.validation.Valid;

/**
 * @author Frank Chen
 * @date 22/12/21 11:17 AM
 */

public interface IEventApi {

    @PostMapping("/api/event/getEventList")
    GetEventListResponse getEventList(@Valid @RequestBody GetEventListRequest request);
}
