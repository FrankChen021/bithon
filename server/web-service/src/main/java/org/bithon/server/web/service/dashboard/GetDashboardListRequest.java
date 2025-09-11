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

package org.bithon.server.web.service.dashboard;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.bithon.server.datasource.query.Order;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/9/11 21:10
 */
@Getter
@Setter
public class GetDashboardListRequest {
    private String folder;
    private String search;

    private Order order = Order.desc;
    private String orderBy = "";

    @Min(0)
    private int pageNumber = 0;

    @Min(5)
    @Max(1000)
    private int pageSize = 10;
}
