package com.keruyun.commons.agent.collector;

import com.keruyun.commons.agent.collector.enums.TransportProtocolEnum;
import com.keruyun.commons.agent.collector.factory.AbstractThriftFactory;
import org.apache.thrift.TBaseProcessor;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.TProcessorFactory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.server.TThreadedSelectorServer;
import org.apache.thrift.transport.*;

/**
 * @author Roy Wesley
 * @date 2017/6/29 17:41
 * @todo Thrift服务端
 */

public class ThriftServer extends AbstractThriftFactory {

    /**
     * 分隔符
     */
    public static final String SPLIT = "$";

    /**
     * 初始化Thrift服务
     *
     * @param baseProcessor 服务进程
     * @param port          服务端口号
     * @param enabled       是否开启SSL
     */
    public void initServer(TBaseProcessor baseProcessor, int port, boolean enabled) {
        if (enabled) {
            initSecureThriftServer(baseProcessor, port, "/.keystore", "Nv4hMRcs6eQ8AmtE");
        } else {
            initThriftServer(baseProcessor, port);
        }
    }

    /**
     * Thrift SSL服务
     *
     * @param baseProcessor 服务进程
     * @param port          服务器端口号
     * @param trustStore    SSL秘钥路径
     * @param password      SSL秘钥密码
     */
    public void initSecureThriftServer(TBaseProcessor baseProcessor, int port, String trustStore, String password) {
        try {
            TSSLTransportFactory.TSSLTransportParameters parameters = new TSSLTransportFactory.TSSLTransportParameters();
            parameters.setKeyStore(this.getClass().getResource(trustStore).toExternalForm(), password, null, null);

            TServerTransport tServerTransport = TSSLTransportFactory.getServerSocket(port, 0, null, parameters);
            TBaseProcessor processor = baseProcessor;
            TThreadPoolServer.Args args = new TThreadPoolServer.Args(tServerTransport);
            args.processorFactory(new TProcessorFactory(processor));
            args.transportFactory(new TFramedTransport.Factory());
            args.protocolFactory(chooseProtocolFactory(TransportProtocolEnum.COMPACT));

            TThreadPoolServer server = new TThreadPoolServer(args);
            if (!server.isServing()) {
                logger.info("start thrift server with SSL on port {} succeed", port);
                server.serve();
            } else {
                logger.warn("the port {} is already in used", port);
                return;
            }
        } catch (TTransportException e) {
            logger.error("start thrift server with SSL on port {} failed", port);
            return;
        }
    }

    /**
     * Thrift单路服务
     *
     * @param port          服务器端口
     * @param baseProcessor 服务进程
     */
    public void initThriftServer(TBaseProcessor baseProcessor, int port) {
        try {
            TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(port);
            TBaseProcessor processor = baseProcessor;
            TThreadedSelectorServer.Args args = new TThreadedSelectorServer.Args(serverTransport);
            args.processorFactory(new TProcessorFactory(processor));
            args.transportFactory(new TFramedTransport.Factory());
            args.protocolFactory(chooseProtocolFactory(TransportProtocolEnum.COMPACT));
            // selector线程只处理对accept的连接的读写事件轮询，除非并发量极大时可以适度调大此值，否则太大会浪费资源
            args.selectorThreads(getProcessNum());

            TThreadedSelectorServer server = new TThreadedSelectorServer(args);
            if (!server.isServing()) {
                logger.info("start thrift server on port {} succeed", port);
                server.serve();
            } else {
                logger.warn("the port {} is already in used", port);
                return;
            }
        } catch (TTransportException e) {
            logger.error("start thrift server on port {} failed", port);
            return;
        }
    }

    /**
     * Thrift多路服务
     *
     * @param port        服务器端口
     * @param tProcessors 服务进程
     */
    public void initThriftServer(int port, TBaseProcessor... tProcessors) {
        try {
            TNonblockingServerTransport serverTransport = new TNonblockingServerSocket(port);
            // 默认是2个selector线程，5个工作线程
            TThreadedSelectorServer.Args args = new TThreadedSelectorServer.Args(serverTransport);

            // 对于非阻塞服务，需要使用TFramedTransport，它将数据分块发送
            args.transportFactory(new TFramedTransport.Factory());
            args.protocolFactory(chooseProtocolFactory(TransportProtocolEnum.COMPACT));
            args.processor(registerProcessor(tProcessors));
            args.maxReadBufferBytes = 10 * 1024 * 1024L;
            // selector线程只处理对accept的连接的读写事件轮询，除非并发量极大时可以适度调大此值，否则太大会浪费资源
            args.selectorThreads(getProcessNum());

            TServer server = new TThreadedSelectorServer(args);
            if (!server.isServing()) {
                logger.info("start thrift server with multiple processor on port {} succeed", port);
                server.serve();
            } else {
                logger.warn("the port {} is already in used", port);
                return;
            }
        } catch (TTransportException e) {
            logger.error("start thrift server with multiple processor on port {} failed", port);
            return;
        }
    }

    /**
     * 多服务注册
     *
     * @param tProcessors 服务数组
     * @return 返回多服务接口处理器
     */
    private TMultiplexedProcessor registerProcessor(TBaseProcessor... tProcessors) {
        // 支持多接口处理器
        TMultiplexedProcessor multiplexedProcessor = new TMultiplexedProcessor();

        if (null == tProcessors) {
            logger.warn("the process {} may be null or empty", tProcessors);
            return null;
        }

        // 遍历注册服务
        for (TProcessor tProcessor : tProcessors) {
            String serviceName = tProcessor.getClass().getSimpleName();
            serviceName = serviceName.substring(0, serviceName.lastIndexOf(SPLIT));
            logger.info("register processor's service Name is: " + serviceName);
            multiplexedProcessor.registerProcessor(serviceName, tProcessor);
        }

        return multiplexedProcessor;
    }

    /**
     * 获取进程数
     *
     * @return 返回进程数
     */
    private int getProcessNum() {
        return Runtime.getRuntime().availableProcessors();
    }
}