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

package org.bithon.server.storage.tracing;

import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.utils.CloseableIterator;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.datasource.query.IDataSourceReader;
import org.bithon.server.datasource.query.Limit;
import org.bithon.server.datasource.query.OrderBy;
import org.bithon.server.datasource.query.result.ColumnarTable;
import org.bithon.server.storage.tracing.mapping.TraceIdMapping;

import java.sql.Timestamp;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 3:28 下午
 */
public interface ITraceReader extends IDataSourceReader {
    CloseableIterator<TraceSpan> getTraceByTraceId(String traceId, TimeSpan start, TimeSpan end);

    List<TraceSpan> getTraceList(IExpression filter,
                                 List<IExpression> indexedTagFilters,
                                 Timestamp start,
                                 Timestamp end,
                                 OrderBy orderBy,
                                 Limit limit);

    ColumnarTable getTraceDistribution(IExpression filter,
                                       List<IExpression> indexedTagFilters,
                                       Timestamp start,
                                       Timestamp end,
                                       long interval);

    int getTraceListSize(IExpression filter,
                         List<IExpression> indexedTagFilters,
                         Timestamp start,
                         Timestamp end);

    List<TraceSpan> getTraceByParentSpanId(String parentSpanId);

    /**
     * Get id mapping by given user id
     */
    TraceIdMapping getTraceIdByMapping(String userId);

}
