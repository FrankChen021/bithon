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

package org.bithon.server.alerting.common.evaluator.metric;

import org.bithon.server.alerting.common.evaluator.EvaluationContext;
import org.bithon.server.alerting.common.evaluator.result.EvaluationOutputs;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.web.service.datasource.api.IDataSourceApi;
import org.bithon.server.web.service.datasource.api.QueryField;

import java.io.IOException;
import java.util.Set;

/**
 * @author frankchen
 * @date 2020-08-21 16:17:30
 */
public interface IMetricEvaluator {

    EvaluationOutputs evaluate(IDataSourceApi dataSourceApi,
                               String dataSource,
                               QueryField metric,
                               TimeSpan start,
                               TimeSpan end,
                               String filterExpression,
                               Set<String> groupBy,
                               EvaluationContext context) throws IOException;
}
