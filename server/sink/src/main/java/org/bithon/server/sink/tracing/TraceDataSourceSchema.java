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

package org.bithon.server.sink.tracing;

import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.metric.DataSourceSchema;
import org.bithon.server.metric.DataSourceSchemaManager;
import org.bithon.server.metric.TimestampSpec;
import org.bithon.server.metric.aggregator.spec.CountMetricSpec;
import org.bithon.server.metric.aggregator.spec.LongSumMetricSpec;
import org.bithon.server.metric.dimension.IDimensionSpec;
import org.bithon.server.metric.dimension.StringDimensionSpec;
import org.bithon.server.sink.tracing.index.TagIndexConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Frank Chen
 * @date 30/1/22 9:56 AM
 */
@Component
public class TraceDataSourceSchema {

    private static final DataSourceSchema TRACE_SPAN_SCHEMA = new DataSourceSchema("trace_span_summary",
                                                                                   "trace_span_summary",
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
                                                                                                 new StringDimensionSpec("status",
                                                                                                                         "status",
                                                                                                                         "status",
                                                                                                                         false,
                                                                                                                         true,
                                                                                                                         null,
                                                                                                                         null),
                                                                                                 new StringDimensionSpec("normalizedUrl",
                                                                                                                         "url",
                                                                                                                         "url",
                                                                                                                         false,
                                                                                                                         true,
                                                                                                                         128,
                                                                                                                         null)),
                                                                                   Arrays.asList(CountMetricSpec.INSTANCE,
                                                                                                 new LongSumMetricSpec("costTimeMs",
                                                                                                                       "costTimeMs",
                                                                                                                       "us",
                                                                                                                       true)
                                                                                   ));
    private final DataSourceSchema TRACE_SPAN_TAG_SCHEMA;

    public TraceDataSourceSchema(DataSourceSchemaManager dataSourceSchemaManager,
                                 TraceConfig traceConfig) {

        // initialize dimensions
        TagIndexConfig indexConfig = traceConfig.getIndexes();
        List<IDimensionSpec> dimensionSpecs = new ArrayList<>();
        if (indexConfig != null && !CollectionUtils.isEmpty(indexConfig.getMap())) {
            for (Map.Entry<String, Integer> entry : indexConfig.getMap().entrySet()) {
                String tagName = entry.getKey();
                Integer indexPos = entry.getValue();
                dimensionSpecs.add(new StringDimensionSpec("f" + indexPos,
                                                           tagName,
                                                           tagName,
                                                           true,
                                                           null,
                                                           null,
                                                           null));
            }
        }

        TRACE_SPAN_TAG_SCHEMA = new DataSourceSchema("trace_span_tag_index",
                                                     "trace_span_tag_index",
                                                     new TimestampSpec("timestamp", null, null),
                                                     dimensionSpecs,
                                                     Collections.singletonList(CountMetricSpec.INSTANCE));

        dataSourceSchemaManager.addListener(new DataSourceSchemaManager.IDataSourceSchemaListener() {
            @Override
            public void onRmv(DataSourceSchema dataSourceSchema) {
            }

            @Override
            public void onAdd(DataSourceSchema dataSourceSchema) {
            }

            @Override
            public void onLoad() {
                dataSourceSchemaManager.addDataSourceSchema(TRACE_SPAN_SCHEMA);
                dataSourceSchemaManager.addDataSourceSchema(TRACE_SPAN_TAG_SCHEMA);
            }
        });
    }

    public static DataSourceSchema getTraceSpanSchema() {
        return TRACE_SPAN_SCHEMA;
    }
}
