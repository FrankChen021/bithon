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

package org.bithon.server.pipeline.common;

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * A simplified executor that support dynamic interval which can be changed on some centralized configuration center such as nacos or apollo.
 *
 * @author frank.chen021@outlook.com
 * @date 2022/11/30 16:40
 */
@Slf4j
public class FixedDelayExecutor {

    private final Thread thread;
    private final Runnable command;
    private final int initialDelay;
    private final Supplier<Duration> delaySupplier;
    private boolean running = true;

    public FixedDelayExecutor(String name,
                              Runnable command,
                              int initDelay,
                              Supplier<Duration> delaySupplier) {
        this.initialDelay = initDelay;
        this.delaySupplier = delaySupplier;
        this.command = command;
        this.thread = new Thread(new Task());
        this.thread.setName(name);
        this.thread.start();
    }

    public void shutdown(Duration wait) throws InterruptedException {
        running = false;
        thread.join(wait.toMillis());
    }

    class Task implements Runnable {
        @Override
        public void run() {
            if (initialDelay > 0) {
                try {
                    Thread.sleep(initialDelay * 1000L);
                } catch (InterruptedException ignored) {
                }
            }

            while (running) {
                try {
                    command.run();
                } catch (Exception e) {
                    log.error(StringUtils.format("Fail to run task [%s]", thread.getName()), e);
                }
                if (running) {
                    try {
                        Thread.sleep(delaySupplier.get().toMillis());
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }
}
