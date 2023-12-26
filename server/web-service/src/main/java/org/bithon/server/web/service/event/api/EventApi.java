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

import org.bithon.component.commons.expression.IExpression;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.event.IEventReader;
import org.bithon.server.storage.event.IEventStorage;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.bithon.server.web.service.datasource.api.FilterExpressionToFilters;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author frank.chen021@outlook.com
 * @date 22/12/21 11:17 AM
 */
@RestController
@Conditional(WebServiceModuleEnabler.class)
public class EventApi implements IEventApi {

    private final IEventReader eventReader;
    private final IEventStorage storage;

    public EventApi(IEventStorage eventStorage) {
        this.eventReader = eventStorage.createReader();
        this.storage = eventStorage;
    }

    @Override
    public GetEventListResponse getEventList(GetEventListRequest request) {
        TimeSpan start = TimeSpan.fromISO8601(request.getStartTimeISO8601());
        TimeSpan end = TimeSpan.fromISO8601(request.getEndTimeISO8601());

        IExpression filter = FilterExpressionToFilters.toExpression(this.storage.getSchema(),
                                                                    null,
                                                                    request.getFilters());

        return new GetEventListResponse(eventReader.getEventListSize(filter,
                                                                     start,
                                                                     end),
                                        eventReader.getEventList(filter,
                                                                 start,
                                                                 end,
                                                                 request.getPageNumber(),
                                                                 request.getPageSize()));
    }
}
