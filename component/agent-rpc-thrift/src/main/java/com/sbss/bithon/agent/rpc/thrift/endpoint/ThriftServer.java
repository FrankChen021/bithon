package com.sbss.bithon.agent.rpc.thrift.endpoint;

import org.apache.thrift.TBaseProcessor;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.layered.TFramedTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author frankchen
 */
public class ThriftServer extends AbstractThriftFactory {
    protected static Logger logger = LoggerFactory.getLogger(AbstractThriftFactory.class);

    public static final String SPLIT = "$";

    public void start(TProcessor baseProcessor, int port) {
        try {
            TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(port);
            TThreadedSelectorServer.Args args = new TThreadedSelectorServer.Args(serverTransport);
            args.processorFactory(new TProcessorFactory(baseProcessor));
            args.transportFactory(new TFramedTransport.Factory());
            args.protocolFactory(createProtocolFactory(TransportProtocolEnum.COMPACT));
            // enlarge the following IO threads only when the traffic is very large
            args.selectorThreads(getProcessorNumbers());

            TThreadedSelectorServer server = new TThreadedSelectorServer(args);
            if (!server.isServing()) {
                new Thread(() -> {
                    logger.info("Starting thrift server on port {}...", port);
                    server.serve();
                    logger.info("Thrift server stopped");
                }, "thrift-server-thread").start();

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    logger.info("Stopping thrift server...");
                    server.stop();
                }));
            } else {
                logger.warn("the port {} is already in used", port);
                return;
            }
        } catch (TTransportException e) {
            logger.error("start thrift server on port {} failed", port);
        }
    }

    public void stop() {

    }

    public void start(int port, TBaseProcessor... tProcessors) {
        try {
            TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(port);
            // 默认是2个selector线程，5个工作线程
            TThreadedSelectorServer.Args args = new TThreadedSelectorServer.Args(serverTransport);

            // 对于非阻塞服务，需要使用TFramedTransport，它将数据分块发送
            args.transportFactory(new TFramedTransport.Factory());
            args.protocolFactory(createProtocolFactory(TransportProtocolEnum.COMPACT));
            args.processor(registerProcessor(tProcessors));
            args.maxReadBufferBytes = 10 * 1024 * 1024L;
            // selector线程只处理对accept的连接的读写事件轮询，除非并发量极大时可以适度调大此值，否则太大会浪费资源
            args.selectorThreads(getProcessorNumbers());

            TServer server = new TThreadedSelectorServer(args);
            if (!server.isServing()) {
                logger.info("start thrift server with multiple processor on port {} succeed", port);
                server.serve();
            } else {
                logger.warn("the port {} is already in used", port);
            }
        } catch (TTransportException e) {
            logger.error("start thrift server with multiple processor on port {} failed", port);
        }
    }

    private TMultiplexedProcessor registerProcessor(TBaseProcessor... tProcessors) {
        TMultiplexedProcessor multiplexedProcessor = new TMultiplexedProcessor();

        if (null == tProcessors) {
            logger.warn("the process may be null or empty");
            return null;
        }

        for (TProcessor tProcessor : tProcessors) {
            String serviceName = tProcessor.getClass().getSimpleName();
            serviceName = serviceName.substring(0, serviceName.lastIndexOf(SPLIT));
            logger.info("register processor's service Name is: " + serviceName);
            multiplexedProcessor.registerProcessor(serviceName, tProcessor);
        }

        return multiplexedProcessor;
    }

    private int getProcessorNumbers() {
        return Runtime.getRuntime().availableProcessors();
    }
}