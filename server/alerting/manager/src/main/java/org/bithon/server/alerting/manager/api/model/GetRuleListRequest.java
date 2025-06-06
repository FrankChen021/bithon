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

package org.bithon.server.alerting.manager.api.model;

import lombok.Data;
import org.bithon.server.datasource.query.Limit;
import org.bithon.server.datasource.query.OrderBy;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/5
 */
@Data
public class GetRuleListRequest {
    private String folder;
    private String alertName;
    private String appName;
    private OrderBy orderBy = new OrderBy();
    private Limit limit = new Limit(10, null);
}
