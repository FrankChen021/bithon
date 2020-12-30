package com.keruyun.commons.agent.collector;

import com.keruyun.commons.agent.collector.enums.TransportProtocolEnum;
import com.keruyun.commons.agent.collector.factory.AbstractThriftFactory;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Roy Wesley
 * @date 2018/1/22 18:29
 * @todo Thrift客户端
 */

public class ThriftClient extends AbstractThriftFactory {

    /**
     * 分隔符
     */
    public static final String SPLIT = "$";

    /**
     * 初始化Thrift服务
     *
     * @param host    服务器地址或域名
     * @param port    服务器端口
     * @param timeout 服务器连接超时时间
     * @param enabled 是否开启SSL
     * @return
     */
    public TTransport initThriftTransport(String host, int port, int timeout, boolean enabled) {
        if (enabled) {
            return initSecureTransport(host, port, timeout, "/.keystore", "Nv4hMRcs6eQ8AmtE");
        } else {
            return initTransport(host, port, timeout);
        }
    }

    /**
     * 初始化SSL传输层
     *
     * @param host       服务器IP地址或域名
     * @param port       服务器端口
     * @param timeout    连接超时时间
     * @param trustStore SSL证书文件地址
     * @param password   SSL证书密码
     * @return
     */
    public TTransport initSecureTransport(String host, int port, int timeout, String trustStore, String password) {
        TTransport transport;
        try {
            TSSLTransportFactory.TSSLTransportParameters parameters = new TSSLTransportFactory.TSSLTransportParameters();
            parameters.setTrustStore(trustStore, password);
            transport = TSSLTransportFactory.getClientSocket(host, port, timeout);
        } catch (Exception e) {
            logger.error("connect server by ssl failed! the exception is {}", e);
            throw new RuntimeException("connect thrift tcp server by ssl failed!", e);
        }

        return transport;
    }

    /**
     * 初始化传输层
     *
     * @param host    服务器IP地址或域名
     * @param port    服务器端口
     * @param timeout 连接服务器超时时间
     * @return 返回传输层
     */
    public TTransport initTransport(String host, int port, int timeout) {
        TTransport transport;
        try {
            transport = new TFramedTransport(new TSocket(host, port, timeout));
            if (!transport.isOpen()) {
                transport.open();
            }
        } catch (Exception e) {
            logger.error("connect server failed! the exception is {}", e);
            throw new RuntimeException("connect thrift tcp server failed!", e);
        }

        return transport;
    }

    /**
     * 获取单服务客户端
     *
     * @param clazz        指定服务客户端
     * @param protocolEnum 协议类型
     * @param tTransport   传输层协议
     * @param <T>          客户端
     * @return 返回客户端
     */
    public <T extends TServiceClient> T getServiceClient(Class<T> clazz, TransportProtocolEnum protocolEnum, TTransport tTransport) {
        TProtocol tProtocol = chooseProtocol(protocolEnum, tTransport);
        return getClient(clazz, tProtocol);
    }

    /**
     * 获取多服务客户端
     *
     * @param clazz        指定服务客户端
     * @param protocolEnum 协议类型
     * @param tTransport   传输层
     * @param <T>          客户端
     * @return 返回客户端
     */
    public <T extends TServiceClient> T getMultServiceClient(Class<T> clazz, TransportProtocolEnum protocolEnum, TTransport tTransport) {
        TProtocol tProtocol = chooseProtocol(protocolEnum, tTransport);
        String serviceName = clazz.getName().trim();
        serviceName = serviceName.substring(0, serviceName.lastIndexOf(SPLIT));
        tProtocol = new TMultiplexedProtocol(tProtocol, serviceName);

        return getClient(clazz, tProtocol);
    }

    /**
     * 根据不同协议获取对应的客户端
     *
     * @param clazz     指定服务客户端
     * @param tProtocol 传输协议
     * @param <T>       客户端
     * @return 返回客户端
     */
    public <T extends TServiceClient> T getClient(Class<T> clazz, TProtocol tProtocol) {
        T serviceClient = null;
        try {
            Constructor<T> constructor = clazz.getConstructor(TProtocol.class);
            constructor.setAccessible(true);

            try {
                serviceClient = constructor.newInstance(tProtocol);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("no such method", e);
        }

        return serviceClient;
    }
}