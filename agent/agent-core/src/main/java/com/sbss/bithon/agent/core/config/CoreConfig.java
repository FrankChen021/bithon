package com.sbss.bithon.agent.core.config;

public class CoreConfig {

    public static CoreConfig INSTANCE = null;

    private boolean debug = false;

    private BootstrapConfig bootstrap;

    private DispatcherConfig dispatcher;

    public BootstrapConfig getBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(BootstrapConfig bootstrap) {
        this.bootstrap = bootstrap;
    }

    public DispatcherConfig getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(DispatcherConfig dispatcher) {
        this.dispatcher = dispatcher;
    }

    public static CoreConfig getInstance() {
        return INSTANCE;
    }

    public static void setInstance(CoreConfig instance) {
        CoreConfig.INSTANCE = instance;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
