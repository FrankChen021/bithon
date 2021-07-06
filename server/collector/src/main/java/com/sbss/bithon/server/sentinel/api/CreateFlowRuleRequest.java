package com.sbss.bithon.server.sentinel.api;

import com.sbss.bithon.agent.sentinel.flow.FlowRuleDto;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/6 7:19 下午
 */
@Data
public class CreateFlowRuleRequest extends FlowRuleDto {
    @NotNull
    private String appName;

    @NotNull
    private String env;
}
