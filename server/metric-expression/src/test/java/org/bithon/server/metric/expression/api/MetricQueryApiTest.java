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

package org.bithon.server.metric.expression.api;


import org.bithon.server.datasource.query.pipeline.ColumnarTable;
import org.bithon.server.datasource.query.pipeline.LongColumn;
import org.bithon.server.datasource.query.pipeline.PipelineQueryResult;
import org.bithon.server.web.service.datasource.api.QueryResponse;
import org.bithon.server.web.service.datasource.api.TimeSeriesMetric;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 11/6/25 10:28 am
 */
public class MetricQueryApiTest {
    @Test
    public void testToQueryResult() {
        long[] timestamp = new long[]{
            1749605220,
            1749605280,
            1749605340,
            1749605400,
            1749605460,
            1749605520,
            1749605580,
            1749605640,
            1749605700,
            1749605760,
            1749605820,
            1749605880,
            1749605940,
            1749606000,
            1749606060,
            1749606120,
            1749606180,
            1749606240,
            1749606300,
            1749606360,
            1749606420,
            1749606480,
            1749606540,
            1749606600,
            1749606660,
            1749606720,
            1749606780,
            1749606840,
            1749606900,
            1749606960,
            1749607020,
            1749607080,
            1749607140,
            1749607200,
            1749607260,
            1749607320,
            1749607380,
            1749607440,
            1749607500,
            1749607560,
            1749607620,
            1749607680,
            1749607740,
            1749607800,
            1749607860,
            1749607920,
            1749607980,
            1749608040,
            1749608100,
            1749608160,
            1749608220,
            1749608280,
            1749608340,
            1749608400,
            1749608460,
            1749608520,
            1749608580,
            1749608640,
            1749608700,
            1749608760,
            };
        long[] values = new long[]{
            342028250,
            504888065,
            275831388,
            354667289,
            235690897,
            197992513,
            346818639,
            211690671,
            450190183,
            371298359,
            360773826,
            366076404,
            276755875,
            319103539,
            192227188,
            210185912,
            357906876,
            220080108,
            384664737,
            210504551,
            282322240,
            399792063,
            293907900,
            299209959,
            231256274,
            462473927,
            375272268,
            436628918,
            428908649,
            396757003,
            235677992,
            514032439,
            322681426,
            298422989,
            355961056,
            277227473,
            367589538,
            219546386,
            302611756,
            309228049,
            437031200,
            220236094,
            386837532,
            311685172,
            247556478,
            337521145,
            271609694,
            470478138,
            322779652,
            341853245,
            349091067,
            274685355,
            427790986,
            259356503,
            282873555,
            322858646,
            263834768,
            203988698,
            341155908,
            418763675,
            };
        PipelineQueryResult result = PipelineQueryResult.builder()
                                                        .keyColumns(List.of("_timestamp"))
                                                        .valColumns(List.of("value"))
                                                        .rows(values.length)
                                                        .table(ColumnarTable.of(new LongColumn("_timestamp", timestamp), new LongColumn("value", values)))
                                                        .build();
        QueryResponse<?> response = MetricQueryApi.toTimeSeriesQueryResponse(1749605160000L,
                                                                             1749608760000L,
                                                                             60000L,
                                                                             result);
        @SuppressWarnings("unchecked")
        Collection<TimeSeriesMetric> metrics = (Collection<TimeSeriesMetric>) response.getData();
        Assertions.assertEquals(1, metrics.size());

        TimeSeriesMetric metric = metrics.iterator().next();
        Assertions.assertEquals(List.of("value"), metric.getTags());
        Assertions.assertEquals(61, metric.getValues().length);
        for (int i = 1; i < values.length; i++) {
            Assertions.assertEquals(values[i - 1], metric.getValues()[i], "Value at index " + i + " does not match");
        }
    }
}
