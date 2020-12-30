package com.sbss.bithon.agent.plugin.quartz2;

import com.sbss.bithon.agent.core.transformer.AbstractClassTransformer;
import com.sbss.bithon.agent.core.transformer.matcher.DefaultMethodNameMatcher;
import com.sbss.bithon.agent.core.transformer.plugin.AgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.IAgentHandler;
import com.sbss.bithon.agent.core.transformer.plugin.MethodPointCut;

/**
 * Description : quartz2 transformer <br>
 * Date: 17/9/4
 *
 * @author 马至远
 */
public class QuartzTransformer extends AbstractClassTransformer {

    @Override
    public IAgentHandler[] getHandlers() {
        return new IAgentHandler[]{new AgentHandler(QuartzHandler.class,
                                                    new MethodPointCut("org.quartz.impl.SchedulerRepository",
                                                                       DefaultMethodNameMatcher.byDefaultCtor())),
            new AgentHandler(QuartzJobExecutionLogHandler.class,
                             new MethodPointCut("org.quartz.core.JobRunShell",
                                                DefaultMethodNameMatcher.byNameAndEmptyArgs("run")))};
    }
}
