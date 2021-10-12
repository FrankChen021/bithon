/*
 *    Copyright 2020 bithon.cn
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
import org.bithon.server.common.utils.ThreadUtils;

import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:55 下午
 */
@Slf4j
public abstract class AbstractThreadPoolMessageHandler<MSG> implements IMessageHandler<MSG> {
    protected final ThreadPoolExecutor executor;

    public AbstractThreadPoolMessageHandler(String name,
                                            int corePoolSize,
                                            int maxPoolSize,
                                            Duration keepAliveTime,
                                            int queueSize) {
        executor = new ThreadPoolExecutor(corePoolSize,
                                          maxPoolSize,
                                          keepAliveTime.getSeconds(),
                                          TimeUnit.SECONDS,
                                          new LinkedBlockingQueue<>(queueSize),
                                          new ThreadUtils.NamedThreadFactory(name),
                                          new ThreadPoolExecutor.DiscardPolicy());
        log.info("Starting executor [{}]", name);

        Thread shutdownThread = new Thread(() -> {
            log.info("Shutting down executor [{}]", this.getType());
            executor.shutdown();
        });
        shutdownThread.setName(name + "-shutdown");
        Runtime.getRuntime().addShutdownHook(shutdownThread);
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
}
