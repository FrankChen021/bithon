package com.sbss.bithon.agent.dispatcher.ktrace;

import com.keruyun.commons.agent.collector.entity.TraceLogEntities;
import com.keruyun.commons.agent.collector.entity.TraceLogEntity;
import com.sbss.bithon.agent.core.config.DispatcherConfig;
import com.sbss.bithon.agent.dispatcher.rpc.RpcClient;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

public class TraceMessageSender {

    private static final Logger log = LoggerFactory.getLogger(TraceMessageSender.class);

    private DispatcherConfig config;
    private FileTraceQueue queue;
    private RpcClient sender;

    private long lastGCTime;
    TraceLogEntities entities;


    /**
     * MetricsMessageSender Constructor, construct the collector & bootstrap the sender
     *
     * @param config            Agent Core Config
     * @param fileQueueInitHook A delay hook function used to init file queue
     */
    public TraceMessageSender(DispatchTraceProcessor processor, Supplier<FileTraceQueue> fileQueueInitHook) {
        entities = new TraceLogEntities();
        entities.setLogs(new ArrayList<>());
        entities.setAppName(processor.appName);
        entities.setHostName(processor.ipAddress);
        this.config = processor.config;
        this.sender = new RpcClient(config);

        // do file queue delayed initialize
        new Timer("infra-ac-trace-starter").schedule(new TimerTask() {
            @Override
            public void run() {
                queue = fileQueueInitHook.get();
                if (null != queue) {
                    //这个时候port才有值
                    entities.setPort(processor.port);

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
                // 为了快速在启动时定位到队列发生的异常, 所以在此处用try-catch包裹逻辑, 记录异常. 同时可以保证线程的成功建立(捕捉bigQueue在消费时可能丢出的异常)
                try {
                    entities.logs.clear();
                    ;
                    for (int i = 0; i < 50; i++) {
                        TraceLogEntity log = (TraceLogEntity) queue.consume();
                        if (log == null)
                            break;
                        if (!isExpiredMessage(log))
                            entities.logs.add(log);
                    }
                    if (!entities.getLogs().isEmpty())
                        sender.sendTrace(entities);

                    gcIfNeed();
                    try {
                        Thread.sleep(50L);
                    } catch (InterruptedException ignored) {
                    }
                } catch (Exception e) {
                    log.error("Sending Entity Failed! \n" + e, e);
                    // processFlag = false;
                }
            }
        }, "infra-ac-trace-sender").start();
    }

    private boolean isExpiredMessage(Object message) {
        boolean expired = true;
        long timestamp = -1;
        if (null != message) {
            if (message instanceof TraceLogEntity) {
                timestamp = ((TraceLogEntity) message).getTimestamp();
            } else {
                log.error(String.format("Invalid timestamp for message: %s", message.getClass().getSimpleName()));
                return true;
            }
            expired = System.currentTimeMillis() - timestamp >= config.getQueue().getValidityPeriod() * 1000;
        }
        return expired;
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
