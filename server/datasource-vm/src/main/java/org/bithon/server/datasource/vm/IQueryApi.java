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

package org.bithon.server.datasource.vm;


import lombok.Data;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 28/4/25 9:42 pm
 */
public interface IQueryApi {

    @Data
    class SeriesData {
        private Map<String, String> metric;
        private List<double[]> values;
    }

    @Data
    class MatrixData {
        private SeriesData[] result;
    }

    @Data
    class RangeQueryResponse {
        /// success
        private String status;
        private MatrixData data;
    }

    @GetMapping("query_range")
    RangeQueryResponse queryRange(@RequestParam("query") String query,
                                  @RequestParam("start") long startTime,
                                  @RequestParam("end") long endTime,
                                  @RequestParam("step") String step);
}
