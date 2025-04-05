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

package org.bithon.server.metric.expression.evaluation;


import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.server.web.service.datasource.api.ColumnarResponse;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.QueryRequest;
import org.bithon.server.web.service.datasource.api.QueryResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * @author frank.chen021@outlook.com
 * @date 4/4/25 3:48 pm
 */
public class MetricExpressionEvaluator implements IEvaluator {
    private final QueryRequest queryRequest;
    private final IDataSourceApi dataSourceApi;
    private final boolean isScalar;

    // Make sure the evaluation is executed ONLY ONCE when the expression is referenced multiple times
    private volatile CompletableFuture<ColumnarResponse> cachedResponse;

    public MetricExpressionEvaluator(QueryRequest queryRequest, IDataSourceApi dataSourceApi) {
        this.queryRequest = queryRequest;
        this.dataSourceApi = dataSourceApi;
        this.isScalar = CollectionUtils.isEmpty(queryRequest.getGroupBy())
                        && (queryRequest.getInterval().getBucketCount() != null && queryRequest.getInterval().getBucketCount() == 1
                            // TODO: judge with STEP and INTERVAL LENGTH
                        );
    }

    @Override
    public boolean isScalar() {
        return isScalar;
    }

    @Override
    public CompletableFuture<ColumnarResponse> evaluate() {
        if (cachedResponse == null) {
            synchronized (this) {
                if (cachedResponse == null) {
                    cachedResponse = CompletableFuture.supplyAsync(() -> {
                        try {
                            QueryResponse<?> response = dataSourceApi.timeseriesV5(queryRequest);
                            return (ColumnarResponse) response.getData();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        }
        return cachedResponse;
    }
}
