package com.sbss.bithon.server.collector;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/10 4:55 下午
 */
@Slf4j
public abstract class AbstractThreadPoolMessageHandler<MSG_HEADER, MSG_BODY> implements IMessageHandler<MSG_HEADER, MSG_BODY> {
    protected final ThreadPoolExecutor executor;

    public AbstractThreadPoolMessageHandler(int corePoolSize,
                                            int maxPoolSize,
                                            Duration keepAliveTime,
                                            int queueSize) {
        executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize,
                                          keepAliveTime.getSeconds(),
                                          TimeUnit.SECONDS,
                                          new LinkedBlockingQueue<>(queueSize),
                                          new ThreadPoolExecutor.DiscardPolicy());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down executor ...");
            executor.shutdown();
        }));
    }

    @Override
    public void submit(MSG_HEADER header, MSG_BODY body) {
        executor.submit(() -> {
            try {
                onMessage(header, body);
            } catch (Exception e) {
                log.error("Error process message", e);
            }
        });
    }

    @Override
    public void submit(MSG_HEADER header, List<MSG_BODY> batchMessages) {
        batchMessages.forEach((message) -> {
            submit(header, message);
        });
    }

    protected abstract void onMessage(MSG_HEADER header, MSG_BODY body) throws Exception;

    protected void execute(Runnable runnable) {
        executor.submit(runnable);
    }
}
