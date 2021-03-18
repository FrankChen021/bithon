package com.sbss.bithon.agent.core.utils.concurrent;

import java.util.concurrent.ThreadFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/18-21:46
 */
public class NamedThreadFactory implements ThreadFactory {
    private final String name;

    public NamedThreadFactory(String name) {
        this.name = name;
    }

    public static ThreadFactory of(String name) {
        return new NamedThreadFactory(name);
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName(this.name);
        return thread;
    }
}
