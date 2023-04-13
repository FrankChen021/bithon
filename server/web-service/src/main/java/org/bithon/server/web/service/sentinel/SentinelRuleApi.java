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

package org.bithon.server.web.service.sentinel;

import lombok.extern.slf4j.Slf4j;
import org.bithon.agent.sentinel.degrade.IDegradingRuleManager;
import org.bithon.agent.sentinel.flow.IFlowRuleManager;
import org.bithon.component.brpc.IServiceController;
import org.bithon.server.collector.cmd.service.AgentServer;
import org.bithon.server.web.service.WebServiceModuleEnabler;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/6 7:13 下午
 */
@Slf4j
@CrossOrigin
@RestController
@Conditional(WebServiceModuleEnabler.class)
public class SentinelRuleApi {

    private final AgentServer commandService;

    public SentinelRuleApi(AgentServer commandService) {
        this.commandService = commandService;
    }

    @PostMapping("/api/sentinel/flow/create")
    public void createFlowRule(@Valid @RequestBody CreateFlowRuleRequest rule) {
        rule.valid();

        //
        // persistent rules
        //

        //
        // dispatch to instances
        //
        commandService.getBrpcServer()
                      .getRemoteServices(rule.getAppName(),
                                         IFlowRuleManager.class)
                      .parallelStream()
                      .forEach(flowRuleManager -> {
                          IServiceController ctrl = (IServiceController) flowRuleManager;
                          log.info("create flow rule on [{}]", ctrl.getPeer());

                          flowRuleManager.create(rule);
                      });
    }

    @PostMapping("/api/sentinel/flow/delete")
    public void deleteFlowRule(@Valid @RequestBody DeleteRuleRequest flowRule) {
        //
        // dispatch to instances
        //
        commandService.getBrpcServer()
                      .getRemoteServices(flowRule.getAppName(),
                                         IFlowRuleManager.class)
                      .parallelStream()
                      .forEach(flowRuleManager -> {
                          IServiceController ctrl = (IServiceController) flowRuleManager;
                          log.info("delete flow rule[{}] on [{}]", flowRule.getRuleId(), ctrl.getPeer());

                          flowRuleManager.delete(flowRule.getRuleId());
                      });
    }

    @PostMapping("/api/sentinel/degrading/create")
    public void createDegradingRule(@Valid @RequestBody CreateDegradingRuleRequest rule) {
        rule.valid();

        //
        // persistent rules
        //

        //
        // dispatch to instances
        //
        commandService.getBrpcServer()
                      .getRemoteServices(rule.getAppName(),
                                         IDegradingRuleManager.class)
                      .parallelStream()
                      .forEach(ruleManager -> {
                          IServiceController ctrl = (IServiceController) ruleManager;
                          log.info("create degrading rule on [{}]", ctrl.getPeer());

                          ruleManager.create(rule);
                      });
    }

    @PostMapping("/api/sentinel/degrading/delete")
    public void deleteDegradingRule(@Valid @RequestBody DeleteRuleRequest flowRule) {
        //
        // dispatch to instances
        //
        commandService.getBrpcServer()
                      .getRemoteServices(flowRule.getAppName(),
                                         IDegradingRuleManager.class)
                      .parallelStream()
                      .forEach(ruleManager -> {
                          IServiceController ctrl = (IServiceController) ruleManager;
                          log.info("delete degrading rule[{}] on [{}]", flowRule.getRuleId(), ctrl.getPeer());

                          ruleManager.delete(flowRule.getRuleId());
                      });
    }
}
