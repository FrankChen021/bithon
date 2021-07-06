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

package com.sbss.bithon.agent.sentinel;

import com.sbss.bithon.agent.sentinel.degrade.DegradeRuleDto;
import com.sbss.bithon.agent.sentinel.flow.FlowRuleDto;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

class ListenerAdaptor implements ISentinelListener {

    @Override
    public void onFlowControlled(HttpServletRequest request) {
    }

    @Override
    public void onFlowRuleLoaded(String source, Collection<FlowRuleDto> rule) {
    }

    @Override
    public void onFlowRuleUnloaded(String source, Collection<FlowRuleDto> rule) {
    }

    @Override
    public void onDegraded(HttpServletRequest request) {
    }

    @Override
    public void onDegradeRuleLoaded(String source, Collection<DegradeRuleDto> rule) {

    }

    @Override
    public void onDegradeRuleUnloaded(String source, Collection<DegradeRuleDto> rule) {

    }
}
