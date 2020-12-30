package com.sbss.bithon.agent.plugin.jvm;

import com.sbss.bithon.agent.core.plugin.AbstractPlugin;

/**
 * @author frankchen
 */
public class JvmPlugin extends AbstractPlugin {

    @Override
    public void start() {
        new JvmMetricService().start();
    }

}
