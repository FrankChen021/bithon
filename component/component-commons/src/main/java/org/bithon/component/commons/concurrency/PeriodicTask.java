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
import java.util.function.Consumer;

/**
 * @author Frank Chen
 * @date 27/2/23 4:40 pm
 */
public class PeriodicTask {
    private final Object locker = new Object();
    private final Task task;
    /**
     * in milliseconds
     */
    private final long period;
    private volatile boolean running = true;
    private final Consumer<Exception> exceptionConsumer;

    @FunctionalInterface
    public interface Task {
        void run() throws Exception;
    }

    public PeriodicTask(String name, Duration period, boolean autoShutdown, Task task, Consumer<Exception> exceptionConsumer) {
        this.task = task;
        this.period = period.toMillis();
        this.exceptionConsumer = exceptionConsumer;

        Thread taskThread = new Thread(this::run);
        taskThread.setName(name);
        taskThread.setDaemon(true);
        taskThread.start();

        if (autoShutdown) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        }
    }

    public void stop() {
        running = false;
        notifyTimeout();
    }

    private void run() {
        while (running) {

            try {
                this.task.run();
            } catch (Exception e) {
                if (exceptionConsumer != null) {
                    exceptionConsumer.accept(e);
                }
            }

            waitTimeout();
        }
    }

    private void waitTimeout() {
        synchronized (locker) {
            try {
                locker.wait(period);
            } catch (InterruptedException ignored) {
            }
        }
    }

    public void notifyTimeout() {
        synchronized (locker) {
            locker.notify();
        }
    }
}
