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

package org.bithon.server.tracing.storage;

import org.bithon.server.common.utils.datetime.TimeSpan;
import org.bithon.server.metric.storage.DimensionCondition;
import org.bithon.server.metric.storage.OrderBy;
import org.bithon.server.tracing.sink.TraceSpan;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 3:28 下午
 */
public interface ITraceReader {
    List<TraceSpan> getTraceByTraceId(String traceId, TimeSpan start, TimeSpan end);

    List<TraceSpan> getTraceList(List<DimensionCondition> filters,
                                 Timestamp start,
                                 Timestamp end,
                                 String orderBy,
                                 String order,
                                 int pageNumber,
                                 int pageSize);

    int getTraceListSize(List<DimensionCondition> filters, Timestamp start, Timestamp end);

    List<TraceSpan> getTraceByParentSpanId(String parentSpanId);

    String getTraceIdByMapping(String id);

    List<TraceSpan> searchTrace(Timestamp start,
                                Timestamp end,
                                Map<String, String> conditions,
                                OrderBy orderBy,
                                int pageNumber,
                                int pageSize);
}
