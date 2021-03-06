package com.sbss.bithon.agent.dispatcher.thrift;

import org.apache.thrift.TApplicationException;
import org.apache.thrift.TServiceClient;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TMultiplexedProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 04:40 下午
 */
public abstract class AbstractThriftClient<T extends TServiceClient> {
    static Logger log = LoggerFactory.getLogger(AbstractThriftClient.class);

    private static final int MAX_RETRY = 3;

    private T client;
    private final int timeout;
    private final String clientName;
    private final List<HostAndPort> servers;
    private int serverSelector = -1;

    static class HostAndPort {
        final String host;
        final int port;

        HostAndPort(String host, int port) {
            this.host = host;
            this.port = port;
        }
    }

    public AbstractThriftClient(String clientName,
                                String servers,
                                int timeout) {
        this.clientName = clientName;
        this.timeout = timeout;
        this.servers = Arrays.stream(servers.split(",")).map((hostAndPortString) -> {
            String[] parts = hostAndPortString.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid servers configuration: " + servers);
            }
            return new HostAndPort(parts[0].trim(), Integer.parseInt(parts[1].trim()));
        }).collect(Collectors.toList());
    }

    public <ReturnType> ReturnType ensureClient(Function<T, ReturnType> callback, int retries) {
        retries = Math.min(retries, MAX_RETRY);

        try {
            T client = openOrCreate();
            if (null == client) {
                //TODO: retry
                return null;
            }

            return callback.apply(client);

        } catch (RuntimeException e) {
            closeClient();
            if (e.getCause() instanceof TTransportException) {
                if (retries > 0) {
                    log.info("(2)retry {} time(s)", MAX_RETRY - retries + 1);
                    return ensureClient(callback, --retries);
                } else {
                    log.error("Failed to send for [{}]: reaching max retries: {}", this.clientName, e.getCause().getMessage());
                }
            } else if (e.getCause() instanceof TApplicationException) {
                log.error(String.format("Application exception occurred when sending for [%s]", this.clientName), e.getCause());
            } else {
                log.error(String.format("Unexpected exception occurred when sending for [%s]", this.clientName), e);
            }
        } catch (Exception e) {
            closeClient();
            log.error(String.format("Unexpected exception occurred when sending for [%s]", this.clientName), e);
        }
        return null;
    }

    private T openOrCreate() {
        if (null == client) {
            HostAndPort hostAndPort = this.servers.get(++serverSelector % servers.size());
            try {
                TTransport transport = new TFramedTransport(new TSocket(hostAndPort.host,
                                                                        hostAndPort.port,
                                                                        timeout));
                if (!transport.isOpen()) {
                    transport.open();
                }

                client = createClient(new TMultiplexedProtocol(new TCompactProtocol(transport),
                                                               clientName));
            } catch (TTransportException e) {
                if (e.getCause() instanceof IOException) {
                    log.error("Failed to connect to server[{}:{}] for [{}], cause:[{}] message:{}",
                              hostAndPort.host,
                              hostAndPort.port,
                              this.clientName,
                              e.getCause().getClass().getSimpleName(),
                              e.getCause().getMessage());
                } else {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return client;
    }

    private void closeClient() {
        try {
            if (null != client && null != client.getInputProtocol() && null != client.getInputProtocol().getTransport()) {
                client.getInputProtocol().getTransport().close();
            }
        } catch (Exception e) {
            log.warn("Failed to close client for [{}]: {}", this.clientName, e.getMessage());
        }

        try {
            if (null != client && null != client.getOutputProtocol() && null != client.getOutputProtocol().getTransport()) {
                client.getOutputProtocol().getTransport().close();
            }
        } catch (Exception e) {
            log.warn("Failed to close client for [{}]: {}", this.clientName, e.getMessage());
        }
        client = null;
    }

    abstract protected T createClient(org.apache.thrift.protocol.TProtocol protocol);
}
