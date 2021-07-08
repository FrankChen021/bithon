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

package com.sbss.bithon.agent.sentinel.degrade;

import com.sbss.bithon.agent.controller.cmd.IAgentCommand;
import com.sbss.bithon.agent.sentinel.SentinelRuleManager;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/5 7:56 下午
 */
public class DegradingRuleManagerImpl implements IDegradingRuleManager, IAgentCommand {
    private static final Logger log = LoggerFactory.getLogger(DegradingRuleManagerImpl.class);

    @Override
    public void create(DegradeRuleDto request) {
        request.valid();
        log.info("Add degrading rule: {}", request);
        SentinelRuleManager.getInstance().addDegradeRule("Command", request, true);
    }

    @Override
    public void update(DegradeRuleDto request) {
        request.valid();
        log.info("Update degrading rule: {}", request);
        SentinelRuleManager.getInstance().updateDegradeRule("Command", request, true);
    }

    @Override
    public void delete(String ruleId) {
        log.info("Delete degrading rule: {}", ruleId);
        SentinelRuleManager.getInstance().deleteDegradeRule("Command", Collections.singletonList(ruleId), true);
    }

    @Override
    public void deleteAll() {
        SentinelRuleManager.getInstance().clearDegradeRules("Command");
    }

    @Override
    public Set<String> getRules() {
        return SentinelRuleManager.getInstance().getDegradeRules();
    }
}
