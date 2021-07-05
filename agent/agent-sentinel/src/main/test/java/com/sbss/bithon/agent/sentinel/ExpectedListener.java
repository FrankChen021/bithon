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

import java.util.Collection;

class ExpectedListener implements ISentinelListener {
    Collection<DegradeRuleDto> loadedDegradeRules;
    Collection<DegradeRuleDto> unLoadedDegradeRules;
    Collection<FlowRuleDto> loadedFlowRules;
    Collection<FlowRuleDto> unLoadedFlowRules;

    @Override
    public void onFlowControlled(String requestURI) {
    }

    @Override
    public void onFlowRuleLoaded(String source, Collection<FlowRuleDto> rules) {
        this.loadedFlowRules = rules;
    }

    @Override
    public void onFlowRuleUnloaded(String source, Collection<FlowRuleDto> rules) {
        this.unLoadedFlowRules = rules;
    }

    @Override
    public void onDegraded(String requestURI) {
    }

    @Override
    public void onDegradeRuleLoaded(String source, Collection<DegradeRuleDto> rules) {
        loadedDegradeRules = rules;
    }

    @Override
    public void onDegradeRuleUnloaded(String source, Collection<DegradeRuleDto> rules) {
        unLoadedDegradeRules = rules;
    }
}
