package com.sbss.bithon.agent.bootstrap.provider;

import java.io.File;

/**
 * Description :
 * <br>Date: 17/11/9
 *
 * @author 马至远
 */
public class AgentPathProvider {
    public static String getAgentPath() {
        return new File(AgentPathProvider.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile().getPath();
    }
}
