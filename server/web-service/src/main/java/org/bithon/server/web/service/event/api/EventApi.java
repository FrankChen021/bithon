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
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.SchemaManager;
import org.bithon.server.storage.datasource.query.IDataSourceReader;
import org.bithon.server.storage.datasource.query.Limit;
import org.bithon.server.storage.datasource.query.Query;
import org.bithon.server.storage.datasource.query.ast.ResultColumn;
import org.bithon.server.storage.event.IEventStorage;
import org.bithon.server.storage.metrics.Interval;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.bithon.server.web.service.datasource.api.FilterExpressionToFilters;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author frank.chen021@outlook.com
 * @date 22/12/21 11:17 AM
 */
@RestController
@Conditional(WebServiceModuleEnabler.class)
@ConditionalOnBean(IEventStorage.class)
public class EventApi implements IEventApi {

    private final ISchema eventTableSchema;

    public EventApi(SchemaManager schemaManager) {
        this.eventTableSchema = schemaManager.getSchema("event");
    }

    @Override
    public GetEventListResponse getEventList(GetEventListRequest request) throws IOException {
        TimeSpan start = TimeSpan.fromISO8601(request.getStartTimeISO8601());
        TimeSpan end = TimeSpan.fromISO8601(request.getEndTimeISO8601());

        IExpression filter = FilterExpressionToFilters.toExpression(this.eventTableSchema,
                                                                    null,
                                                                    request.getFilters());
        try (IDataSourceReader reader = eventTableSchema.getDataStoreSpec().createReader()) {
            Query query = Query.builder()
                               .schema(eventTableSchema)
                               .resultColumns(Arrays.asList(new ResultColumn("appName"),
                                                            new ResultColumn("instanceName"),
                                                            new ResultColumn("type"),
                                                            new ResultColumn("arguments"),
                                                            new ResultColumn("timestamp")))
                               .filter(filter)
                               .limit(new Limit(request.getPageSize(), request.getPageSize() * request.getPageNumber()))
                               .interval(Interval.of(start, end))
                               .build();

            return new GetEventListResponse(reader.listSize(query),
                                            reader.list(query));
        }
    }
}
