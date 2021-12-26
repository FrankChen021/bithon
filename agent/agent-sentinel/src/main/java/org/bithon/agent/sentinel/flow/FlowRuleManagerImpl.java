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

package org.bithon.agent.sentinel.flow;

import org.bithon.agent.controller.cmd.IAgentCommand;
import org.bithon.agent.sentinel.SentinelRuleManager;
import org.bithon.component.logging.ILogAdaptor;
import org.bithon.component.logging.LoggerFactory;

import java.util.Collections;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/5 7:54 下午
 */
public class FlowRuleManagerImpl implements IFlowRuleManager, IAgentCommand {
    private static final ILogAdaptor log = LoggerFactory.getLogger(FlowRuleManagerImpl.class);

    @Override
    public void create(FlowRuleDto request) {
        request.valid();
        log.info("Add flow rule: {}", request);
        SentinelRuleManager.getInstance().addFlowControlRule("Command", request, true);
    }

    @Override
    public void update(FlowRuleDto request) {
        request.valid();
        log.info("Update flow rule: {}", request);
        SentinelRuleManager.getInstance().updateFlowRule("Command", request, true);
    }

    @Override
    public void delete(String ruleId) {
        log.info("Delete flow rule: {}", ruleId);
        SentinelRuleManager.getInstance().deleteFlowRule("Command", Collections.singletonList(ruleId), true);
    }

    @Override
    public void deleteAll() {
        SentinelRuleManager.getInstance().clearFlowRules("Command");
    }

    @Override
    public Set<String> getRules() {
        return SentinelRuleManager.getInstance().getFlowRules();
    }
}
