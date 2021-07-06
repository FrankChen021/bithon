package com.sbss.bithon.server.sentinel.api;

import com.sbss.bithon.agent.sentinel.flow.FlowRuleDto;
import com.sbss.bithon.server.cmd.CommandService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/6 7:13 下午
 */
@CrossOrigin
@RestController
public class SentinelFlowApi {

    private CommandService commandService;

    @PostMapping("/api/sentinel/flow/create")
    public void createFlowRule(@RequestBody CreateFlowRuleRequest flowRule) {
        flowRule.valid();

        //
        // persistent rules
        //

        //
        // send to instances
        //
    }

    @PostMapping("/api/sentinel/flow/delete")
    public void delete(@RequestParam @NotNull String id) {
    }
}
