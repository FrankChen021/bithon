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

package org.bithon.server.common.handler;

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.server.sink.event.LocalEventSink;
import org.bithon.server.sink.metrics.LocalMetricSink;
import org.bithon.server.sink.tracing.LocalTraceSink;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:55 下午
 */
@Slf4j
public abstract class AbstractThreadPoolMessageHandler<MSG> implements IMessageHandler<MSG>, AutoCloseable {
    protected final ThreadPoolExecutor executor;

    public AbstractThreadPoolMessageHandler(String name,
                                            int corePoolSize,
                                            int maxPoolSize,
                                            Duration keepAliveTime,
                                            int queueSize) {
        log.info("Starting executor [{}]", name);
        executor = new ThreadPoolExecutor(corePoolSize,
                                          maxPoolSize,
                                          keepAliveTime.getSeconds(),
                                          TimeUnit.SECONDS,
                                          new LinkedBlockingQueue<>(queueSize),
                                          NamedThreadFactory.of(name),
                                          new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    @Override
    public void submit(MSG msg) {
        executor.submit(() -> {
            try {
                onMessage(msg);
            } catch (Exception e) {
                log.error("Error process message", e);
            }
        });
    }

    protected abstract void onMessage(MSG msg) throws Exception;

    /**
     * @see LocalMetricSink#close()
     * @see LocalTraceSink#close()
     * @see LocalEventSink#close()
     */
    @Override
    public void close() throws Exception {
        log.info("Shutting down executor [{}]", this.getType());
        executor.shutdown();
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
            log.info("Shutting down executor [{}] timeout.", this.getType());
        }
    }
}
