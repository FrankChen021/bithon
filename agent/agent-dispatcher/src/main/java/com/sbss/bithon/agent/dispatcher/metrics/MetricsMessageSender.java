package com.sbss.bithon.agent.dispatcher.metrics;

import com.keruyun.commons.agent.collector.entity.*;
import com.sbss.bithon.agent.core.config.DispatcherConfig;
import com.sbss.bithon.agent.dispatcher.rpc.RpcClient;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

public class MetricsMessageSender {

    private static final Logger log = LoggerFactory.getLogger(MetricsMessageSender.class);

    private DispatcherConfig config;

    private FileQueue queue;

    private RpcClient sender;

    private long lastGCTime;

    /**
     * MetricsMessageSender Constructor, construct the collector & bootstrap the sender
     *
     * @param config            Agent Core Config
     * @param fileQueueInitHook A delay hook function used to init file queue
     */
    public MetricsMessageSender(DispatcherConfig config,
                                Supplier<FileQueue> fileQueueInitHook,
                                RpcClient sender) {
        this.config = config;
        this.sender = sender;
        // do file queue delayed initialize
        new Timer("infra-ac-metrics-starter").schedule(new TimerTask() {
            @Override
            public void run() {
                queue = fileQueueInitHook.get();
                if (null != queue) {
                    // start collector work
                    lastGCTime = System.currentTimeMillis();
                    consumeAndSend();
                    this.cancel();
                }
            }
        }, 0, 100);
    }

    private void consumeAndSend() {
        log.info("Listening messages in file queue for consuming");
        new Thread(() -> {
            // 用于标识当前线程状态, 如果线程捕捉到异常, 则将状态置为false, 终止线程
            boolean processFlag = true;
            while (processFlag) {
                // 为了快速在启动时定位到队列发生的异常, 所以在此处用try-catch包裹逻辑, 记录异常.
                // 同时可以保证线程的成功建立(捕捉bigQueue在消费时可能丢出的异常)
                try {
                    Object message = queue.consume();
                    if (!isExpiredMessage(message)) {
                        sender.send(message);
                    }
                    gcIfNeed();
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException ignored) {
                    }
                } catch (Exception e) {
                    log.error("Sending Entity Failed! \n" + e, e);
                    // processFlag = false;
                }
            }
        }, "infra-ac-metrics-sender").start();
    }

    private boolean isExpiredMessage(Object message) {
        boolean result = true;
        long timestamp = -1;
        if (null != message) {
            if (message instanceof InstanceInfoEntity) {
                timestamp = ((InstanceInfoEntity) message).getTimestamp();
            } else if (message instanceof ServerInfoEntity) {
                timestamp = ((ServerInfoEntity) message).getTimestamp();
            } else if (message instanceof RequestInfoEntity) {
                timestamp = ((RequestInfoEntity) message).getTimestamp();
            } else if (message instanceof SqlInfoEntity) {
                timestamp = ((SqlInfoEntity) message).getTimestamp();
            } else if (message instanceof JdbcConnectionEntity) {
                timestamp = ((JdbcConnectionEntity) message).getTimestamp();
            } else if (message instanceof QuartzEntity) {
                timestamp = ((QuartzEntity) message).getTimestamp();
            } else if (message instanceof ExtendEntity) {
                timestamp = ((ExtendEntity) message).getTimestamp();
            } else if (message instanceof HttpEntity) {
                timestamp = ((HttpEntity) message).getTimestamp();
            } else if (message instanceof DetailEntity) {
                timestamp = System.currentTimeMillis();
            } else if (message instanceof QuartzInfoEntity) {
                timestamp = System.currentTimeMillis();
            } else if (message instanceof MiddlewareEntity) {
                MiddlewareEntity tempMessage = (MiddlewareEntity) message;
                if (null != tempMessage.getRedisEntity()) {
                    timestamp = tempMessage.getRedisEntity().getTimestamp();
                } else if (null != tempMessage.getMongoDBEntity()) {
                    timestamp = tempMessage.getMongoDBEntity().getTimestamp();
                } else if (null != tempMessage.getMysqlEntity()) {
                    timestamp = tempMessage.getMysqlEntity().getTimestamp();
                }
            } else if (message instanceof AppKafkaEntity) {
                timestamp = ((AppKafkaEntity) message).getTimestamp();
            } else if (message instanceof FailureMessageEntity) {
                timestamp = System.currentTimeMillis();
            } else if (message instanceof SpringRestfulUriPatternEntity) {
                timestamp = ((SpringRestfulUriPatternEntity) message).getTimestamp();
            } else if (message instanceof KMetricsEntity) {
                timestamp = ((KMetricsEntity) message).getTimestamp();
            } else if (message instanceof KafkaClientEntity) {
                timestamp = ((KafkaClientEntity) message).getTimestamp();
            } else if (message instanceof ClientPropertiesEntity) {
                timestamp = ((ClientPropertiesEntity) message).getTimestamp();
            } else {
                log.error(String.format("Invalid timestamp for message: %s", message.getClass().getSimpleName()));
            }
            result = System.currentTimeMillis() - timestamp >= config.getQueue().getValidityPeriod() * 1000;
        }
        return result;
    }

    private void gcIfNeed() {
        long sysTime = System.currentTimeMillis();
        if (sysTime - lastGCTime >= config.getQueue().getGcPeriod() * 1000) {
            queue.gc();
            lastGCTime = sysTime;
            log.debug("GC for {}, current size: {}", queue.getName(), queue.size());
        }
    }
}
