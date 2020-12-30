package com.sbss.bithon.agent.core.config;

import java.util.List;

public class BootstrapConfig {

    private String env;

    private String appName;

    private List<String> appLogs;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public List<String> getAppLogs() {
        return appLogs;
    }

    public void setAppLogs(List<String> appLogs) {
        this.appLogs = appLogs;
    }


    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }
}
