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

    private static final DataSourceSchema SCHEMA = new DataSourceSchema("trace_span",
                                                                        "trace_span",
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
                                                                                      new StringDimensionSpec("kind",
                                                                                                              "kind",
                                                                                                              false,
                                                                                                              null,
                                                                                                              null,
                                                                                                              null)),
                                                                        Collections.singletonList(CountMetricSpec.INSTANCE),
                                                                        null,
                                                                        null);

    public static DataSourceSchema getSchema() {
        return SCHEMA;
    }
}
