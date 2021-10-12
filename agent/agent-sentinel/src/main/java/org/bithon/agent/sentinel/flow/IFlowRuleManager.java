/*
 *    Copyright 2020 bithon.cn
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

package org.bithon.agent.sentinel.flow;

import org.bithon.component.brpc.ServiceConfig;
import org.bithon.component.brpc.message.serializer.Serializer;

import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/5 7:50 下午
 */
public interface IFlowRuleManager {

    @ServiceConfig(serializer = Serializer.JSON)
    void create(FlowRuleDto request);

    @ServiceConfig(serializer = Serializer.JSON)
    void update(FlowRuleDto request);

    void delete(String ruleId);

    void deleteAll();

    Set<String> getRules();
}
