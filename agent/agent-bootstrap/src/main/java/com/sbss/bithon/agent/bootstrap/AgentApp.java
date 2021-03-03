package com.sbss.bithon.agent.bootstrap;

import java.lang.instrument.Instrumentation;

/**
 * @author frankchen
 */
public class AgentApp {
    public static void premain(String agentArgs,
                               Instrumentation inst) throws Exception {
        new AgentStarter().start(inst);
    }
}
