package com.sbss.bithon.agent.core.tracing.config;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/5 21:33
 */
public class TraceConfig {

    /**
     * in range of [0, 100]
     */
    private int samplingRate = 0;

    public int getSamplingRate() {
        return samplingRate;
    }

    public void setSamplingRate(int samplingRate) {
        this.samplingRate = samplingRate;
    }
}
