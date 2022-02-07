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

package org.bithon.server.tracing;

import org.bithon.server.metric.DataSourceSchema;
import org.bithon.server.metric.TimestampSpec;
import org.bithon.server.metric.aggregator.spec.CountMetricSpec;
import org.bithon.server.metric.dimension.StringDimensionSpec;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author Frank Chen
 * @date 30/1/22 9:56 AM
 */
public class TraceDataSourceSchema {

    private static final DataSourceSchema SCHEMA = new DataSourceSchema("trace_span_summary",
                                                                        "trace_span_summary",
                                                                        new TimestampSpec("timestamp", null, null),
                                                                        Arrays.asList(new StringDimensionSpec("appName",
                                                                                                              "appName",
                                                                                                              true,
                                                                                                              null,
                                                                                                              null,
                                                                                                              null),
                                                                                      new StringDimensionSpec("instanceName",
                                                                                                              "instanceName",
                                                                                                              false,
                                                                                                              null,
                                                                                                              null,
                                                                                                              null),
                                                                                      new StringDimensionSpec("status",
                                                                                                              "status",
                                                                                                              false,
                                                                                                              true,
                                                                                                              null,
                                                                                                              null)),
                                                                        Collections.singletonList(CountMetricSpec.INSTANCE),
                                                                        null,
                                                                        null);

    public static DataSourceSchema getSchema() {
        return SCHEMA;
    }
}
