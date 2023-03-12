/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.component.commons.concurrency;

import java.time.Duration;

/**
 * @author Frank Chen
 * @date 27/2/23 4:40 pm
 */
public abstract class PeriodicTask {
    private final Object locker = new Object();

    /**
     * in milliseconds
     */
    private final long period;

    private final String name;

    private volatile boolean running = true;
    private final boolean autoShutdown;

    public PeriodicTask(String name,
                        Duration period,
                        boolean autoShutdown) {
        this.period = period.toMillis();
        this.name = name;
        this.autoShutdown = autoShutdown;
    }

    public String getName() {
        return name;
    }

    public final void start() {
        Thread taskThread = new Thread(this::schedule);
        taskThread.setName(name);
        taskThread.setDaemon(true);
        taskThread.start();

        if (autoShutdown) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        }
    }

    public final void stop() {
        running = false;
        runTask();
    }

    private void schedule() {
        while (running) {

            try {
                this.onRun();
            } catch (Exception e) {
                onException(e);
            }

            waitTimeout();
        }

        onStopped();
    }

    private void waitTimeout() {
        synchronized (locker) {
            try {
                locker.wait(period);
            } catch (InterruptedException ignored) {
            }
        }
    }

    /**
     * Run current task immediately.
     */
    public final void runTask() {
        synchronized (locker) {
            locker.notify();
        }
    }

    protected abstract void onRun() throws Exception;

    protected abstract void onException(Exception e);

    protected abstract void onStopped();
}
