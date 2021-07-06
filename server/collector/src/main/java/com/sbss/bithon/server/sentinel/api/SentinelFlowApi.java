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

package com.sbss.bithon.server.sentinel.api;

import com.sbss.bithon.agent.sentinel.flow.IFlowRuleManager;
import com.sbss.bithon.component.brpc.IServiceController;
import com.sbss.bithon.server.cmd.CommandService;
import lombok.extern.slf4j.Slf4j;
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
public class SentinelFlowApi {

    private CommandService commandService;

    @PostMapping("/api/sentinel/flow/create")
    public void createFlowRule(@Valid @RequestBody CreateFlowRuleRequest flowRule) {
        flowRule.valid();

        //
        // persistent rules
        //

        //
        // dispatch to instances
        //
        commandService.getServerChannel()
                      .getRemoteService(flowRule.getAppName(),
                                        IFlowRuleManager.class)
                      .parallelStream()
                      .forEach(flowRuleManager -> {
                          IServiceController ctrl = (IServiceController) flowRuleManager;
                          log.info("create flow rule on [{}]", ctrl.getPeer());

                          flowRuleManager.create(flowRule);
                      });
    }

    @PostMapping("/api/sentinel/flow/delete")
    public void delete(@Valid @RequestBody DeleteFlowRuleRequest flowRule) {
        //
        // dispatch to instances
        //
        commandService.getServerChannel()
                      .getRemoteService(flowRule.getAppName(),
                                        IFlowRuleManager.class)
                      .parallelStream()
                      .forEach(flowRuleManager -> {
                          IServiceController ctrl = (IServiceController) flowRuleManager;
                          log.info("delete flow rule[{}] on [{}]", flowRule.getRuleId(), ctrl.getPeer());

                          flowRuleManager.delete(flowRule.getRuleId());
                      });
    }
}
