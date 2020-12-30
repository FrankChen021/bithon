package com.sbss.bithon.agent.core.utils.time;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/5 9:13 下午
 */
public class Clock {
    final long millis;
    final long nanos;

    public Clock() {
        this.millis = System.currentTimeMillis();
        this.nanos = System.nanoTime();
    }

    public long currentMicroseconds() {
        return (System.nanoTime() - this.nanos) / 1000L + this.millis * 1000;
    }

    public long currentMilliseconds() {
        return this.millis;
    }

    @Override
    public String toString() {
        return "Clock{millis=" + this.millis + ", nanos=" + this.nanos + "}";
    }
}
