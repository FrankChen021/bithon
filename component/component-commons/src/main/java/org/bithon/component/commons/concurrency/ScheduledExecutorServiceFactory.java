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

import org.bithon.component.commons.forbidden.SuppressForbidden;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * An exception safe version of {@link java.util.concurrent.ScheduledThreadPoolExecutor}.
 * It's safe for the scheduled task to throw exception while the task can be further scheduled.
 *
 * @author Frank Chen
 * @date 5/9/23 11:11 am
 */
@SuppressForbidden
public class ScheduledExecutorServiceFactory {
    private static final ILogAdaptor LOGGER = LoggerFactory.getLogger(ScheduledExecutorServiceFactory.class);

    static class ExceptionSafeRunnable implements Runnable {
        final Runnable runnable;

        ExceptionSafeRunnable(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run() {
            try {
                runnable.run();
            } catch (Throwable t) {
                LOGGER.error("Exception when executing task", t);
            }
        }
    }

    static class ExceptionSafeScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {

        public ExceptionSafeScheduledThreadPoolExecutor(int corePoolSize) {
            super(corePoolSize);
        }

        public ExceptionSafeScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
            super(corePoolSize, threadFactory);
        }

        public ExceptionSafeScheduledThreadPoolExecutor(int corePoolSize, RejectedExecutionHandler handler) {
            super(corePoolSize, handler);
        }

        public ExceptionSafeScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
            super(corePoolSize, threadFactory, handler);
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return super.schedule(new ExceptionSafeRunnable(command), delay, unit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            return super.schedule(callable, delay, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return super.scheduleAtFixedRate(new ExceptionSafeRunnable(command), initialDelay, period, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            return super.scheduleWithFixedDelay(new ExceptionSafeRunnable(command), initialDelay, delay, unit);
        }
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
        return new ExceptionSafeScheduledThreadPoolExecutor(1);
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
        return new ExceptionSafeScheduledThreadPoolExecutor(1, threadFactory);
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
        return new ExceptionSafeScheduledThreadPoolExecutor(corePoolSize);
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory) {
        return new ExceptionSafeScheduledThreadPoolExecutor(corePoolSize, threadFactory);
    }
}
