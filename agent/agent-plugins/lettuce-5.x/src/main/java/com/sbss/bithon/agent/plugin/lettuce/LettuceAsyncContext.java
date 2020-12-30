package com.sbss.bithon.agent.plugin.lettuce;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/27 5:35 下午
 */
public class LettuceAsyncContext {
    private String endpoint;
    private Long startNano;

    public void setStartTime(long startNano) {
        this.startNano = startNano;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Long getStartTime() {
        return startNano;
    }
}
