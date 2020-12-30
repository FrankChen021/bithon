package com.sbss.bithon.agent.core.setting;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 3:39 下午
 */
public enum SettingRootNames {
    SQL("sql");

    private final String name;

    SettingRootNames(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
