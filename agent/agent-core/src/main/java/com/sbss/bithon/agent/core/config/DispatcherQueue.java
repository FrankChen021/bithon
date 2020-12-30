package com.sbss.bithon.agent.core.config;

/**
 * @author frankchen
 */
public class DispatcherQueue {

    private int validityPeriod;

    private int gcPeriod;

    public int getValidityPeriod() {
        return validityPeriod;
    }

    public void setValidityPeriod(int validityPeriod) {
        this.validityPeriod = validityPeriod;
    }

    public int getGcPeriod() {
        return gcPeriod;
    }

    public void setGcPeriod(int gcPeriod) {
        this.gcPeriod = gcPeriod;
    }
}
