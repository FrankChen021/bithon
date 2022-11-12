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

package org.bithon.server.web.service.event.api;

import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.matcher.StringEqualMatcher;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.event.IEventReader;
import org.bithon.server.storage.event.IEventStorage;
import org.bithon.server.storage.metrics.DimensionFilter;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

/**
 * @author frank.chen021@outlook.com
 * @date 22/12/21 11:17 AM
 */
@RestController
@Conditional(WebServiceModuleEnabler.class)
public class EventApi implements IEventApi {

    private final IEventReader eventReader;

    public EventApi(IEventStorage eventStorage) {
        this.eventReader = eventStorage.createReader();
    }

    @Override
    public GetEventListResponse getEventList(GetEventListRequest request) {
        TimeSpan start = TimeSpan.fromISO8601(request.getStartTimeISO8601());
        TimeSpan end = TimeSpan.fromISO8601(request.getEndTimeISO8601());

        // backward compatibility
        if (StringUtils.hasText(request.getApplication())) {
            request.setFilters(new ArrayList<>(request.getFilters()));
            request.getFilters().add(new DimensionFilter("appName", new StringEqualMatcher(request.getApplication())));
        }

        return new GetEventListResponse(eventReader.getEventListSize(request.getFilters(),
                                                                     start,
                                                                     end),
                                        eventReader.getEventList(request.getFilters(),
                                                                 start,
                                                                 end,
                                                                 request.getPageNumber(),
                                                                 request.getPageSize()));
    }
}
