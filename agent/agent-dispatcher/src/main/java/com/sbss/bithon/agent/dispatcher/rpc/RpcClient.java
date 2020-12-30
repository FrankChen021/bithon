package com.sbss.bithon.agent.dispatcher.rpc;

import com.keruyun.commons.agent.collector.entity.TraceLogEntities;
import com.keruyun.commons.agent.collector.service.AgentService.Client;
import com.keruyun.commons.agent.collector.service.EntitySendingProxy;
import com.sbss.bithon.agent.core.config.DispatcherConfig;
import com.sbss.bithon.agent.core.config.DispatcherServer;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lizheng
 */
public class RpcClient {

    private static final Logger log = LoggerFactory.getLogger(RpcClient.class);

    private static final int MAX_RETRY = 3;

    private DispatcherConfig config;

    private DispatcherServer serverConfig;

    private Client client;

    private Lock lock = new ReentrantLock();

    private EntitySendingProxy sendingProxy;

    public RpcClient(DispatcherConfig config) {
        this.config = config;
        if (null != config.getServers() && !config.getServers().isEmpty()) {
            this.serverConfig = config.getServers().iterator().next();
        }
        sendingProxy = new EntitySendingProxy();
//        retrofit = new Retrofit.Builder().baseUrl(new StringBuilder("http://").append(serverConfig.getHost()).append(":").append(serverConfig.getHttpPort()).append("/agent/").toString()).build();
    }

    public void send(Object entity) {
        lock.lock();
        send(entity, MAX_RETRY);
        lock.unlock();
    }

    private void send(Object entity, int retries) {
        try {
            openClient();
            if (null == client) {
                return;
            }
            String response = sendingProxy.send(client, entity);
            log.debug("Send [{}] response: {}", entity.getClass().getSimpleName(), response);
        } catch (TTransportException e) {
            closeClient();
            if (retries > 0) {
                log.info("(1)retry {} time(s)", MAX_RETRY - retries + 1);
                send(entity, --retries);
            } else {
                log.error("Send failed(1): {}", entity.getClass().getSimpleName(), e);
            }
        } catch (InvocationTargetException e) {
            closeClient();
            if (e.getTargetException() instanceof TTransportException) {
                if (retries > 0) {
                    log.info("(2)retry {} time(s)", MAX_RETRY - retries + 1);
                    send(entity, --retries);
                } else {
                    log.error("Send failed(2): {}", entity.getClass().getSimpleName(), e);
                }
            } else {
                log.error("Send failed(3): {}", entity.getClass().getSimpleName(), e);
            }
        } catch (Exception e) {
            closeClient();
            log.error("Process failed: {}", entity.getClass().getSimpleName(), e);
        }
    }

    public void sendTrace(TraceLogEntities logs) {
        try {
            openClient();
            if (null == client) {
                log.error("Client is null, send canceled");
                return;
            }
            client.writeTraceLogs(logs);

        } catch (TTransportException e) {
            closeClient();
            log.error("Send trace failed: {}", e);

        } catch (Exception e) {
            closeClient();
            log.error("Process trace failed: {}", e);
        }
    }

    public String getAgentConfig(String appName) {
        try {
            lock.lock();
            openClient();
            if (null == client) {
                log.error("Client is null");
                return null;
            }
            return client.getAgentConfig(appName);
        } catch (Exception e) {
            closeClient();
            log.error("fetch config failed: {}", e);
        } finally {
            lock.unlock();
        }
        return null;
    }

    private void closeClient() {
        try {
            if (null != client && null != client.getInputProtocol() && null != client.getInputProtocol().getTransport()) {
                client.getInputProtocol().getTransport().close();
            }
            if (null != client && null != client.getOutputProtocol() && null != client.getOutputProtocol().getTransport()) {
                client.getOutputProtocol().getTransport().close();
            }
            client = null;
        } catch (Exception e) {
            log.error("closeClient fail", e);
            client = null;
        }
    }

    private Client openClient() {
        boolean needCreate = (null == client);
        log.debug("need create client: " + needCreate);
        if (needCreate) {
            try {
                TTransport transport = new TFramedTransport(new TSocket(serverConfig.getHost(), serverConfig.getPort(), config.getClient().getTimeout()));
                if (!transport.isOpen()) {
                    transport.open();
                }
                client = new Client(new TCompactProtocol(transport));
            } catch (TTransportException e) {
                if (e.getCause() instanceof ConnectException) {
                    log.error("failed to connect to agent server:{}", e.getCause().getMessage());
                } else {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return client;
    }
}