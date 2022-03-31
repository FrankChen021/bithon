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
import org.bithon.server.storage.datasource.aggregator.spec.CountMetricSpec;
import org.bithon.server.storage.datasource.dimension.StringDimensionSpec;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author Frank Chen
 * @date 30/1/22 11:53 AM
 */
public class EventDataSourceSchema {

    private static final DataSourceSchema SCHEMA = new DataSourceSchema("event",
                                                                        "event",
                                                                        new TimestampSpec("timestamp", null, null),
                                                                        Arrays.asList(new StringDimensionSpec("appName",
                                                                                                              "appName",
                                                                                                              "appName",
                                                                                                              true,
                                                                                                              null,
                                                                                                              null,
                                                                                                              null),
                                                                                      new StringDimensionSpec("instanceName",
                                                                                                              "instanceName",
                                                                                                              "instanceName",
                                                                                                              false,
                                                                                                              null,
                                                                                                              null,
                                                                                                              null),
                                                                                      new StringDimensionSpec("type",
                                                                                                              "type",
                                                                                                              "type",
                                                                                                              false,
                                                                                                              true,
                                                                                                              null,
                                                                                                              null)),
                                                                        Collections.singletonList(CountMetricSpec.INSTANCE));

    public static DataSourceSchema getSchema() {
        return SCHEMA;
    }
}
