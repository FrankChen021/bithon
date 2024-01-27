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

package org.bithon.server.storage.event;

import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.TimestampSpec;
import org.bithon.server.storage.datasource.column.StringColumn;
import org.bithon.server.storage.datasource.column.aggregatable.count.AggregateCountColumn;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author Frank Chen
 * @date 27/1/24 12:13 pm
 */
public class EventSchema {
    public static DataSourceSchema createEventTableSchema(IEventStorage eventStorage) {
        return new DataSourceSchema("event",
                                    "event",
                                    new TimestampSpec("timestamp", null, null),
                                    Arrays.asList(new StringColumn("appName",
                                                                   "appName"),
                                                  new StringColumn("instanceName",
                                                                   "instanceName"),
                                                  new StringColumn("type",
                                                                   "type")),
                                    Collections.singletonList(AggregateCountColumn.INSTANCE),

                                    // inputSource
                                    null,
                                    new EventDataStoreSpec("bithon_event", eventStorage),
                                    null,
                                    null);
    }
}
