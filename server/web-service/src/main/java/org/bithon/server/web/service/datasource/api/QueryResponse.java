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

package org.bithon.server.web.service.datasource.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bithon.server.storage.datasource.query.Limit;

import java.util.Collection;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/7 13:08
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResponse {

    /**
     * The number of total records that satisfies the request conditions.
     * Only available when the request is performed on page 0
     */
    private Integer total;

    private Limit limit;
    private long startTimestamp;

    /**
     * Input end timestamp
     */
    private long endTimestamp;

    /**
     * in milliseconds
     */
    private long interval;
    private Collection<?> data;
}
