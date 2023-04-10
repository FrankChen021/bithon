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

package org.bithon.component.brpc.channel;

import org.bithon.component.brpc.IServiceRegistry;
import org.bithon.component.brpc.ServiceRegistry;
import org.bithon.component.brpc.ServiceRegistryItem;
import org.bithon.component.brpc.endpoint.EndPoint;
import org.bithon.component.brpc.endpoint.IEndPointProvider;
import org.bithon.component.brpc.exception.CallerSideException;
import org.bithon.component.brpc.exception.ChannelException;
import org.bithon.component.brpc.exception.ServiceNotFoundException;
import org.bithon.component.brpc.invocation.InvocationManager;
import org.bithon.component.brpc.invocation.ServiceStubFactory;
import org.bithon.component.brpc.message.Headers;
import org.bithon.component.brpc.message.in.ServiceMessageInDecoder;
import org.bithon.component.brpc.message.out.ServiceMessageOutEncoder;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.shaded.io.netty.bootstrap.Bootstrap;
import org.bithon.shaded.io.netty.channel.Channel;
import org.bithon.shaded.io.netty.channel.ChannelHandlerContext;
import org.bithon.shaded.io.netty.channel.ChannelInboundHandlerAdapter;
import org.bithon.shaded.io.netty.channel.ChannelInitializer;
import org.bithon.shaded.io.netty.channel.ChannelOption;
import org.bithon.shaded.io.netty.channel.ChannelPipeline;
import org.bithon.shaded.io.netty.channel.nio.NioEventLoopGroup;
import org.bithon.shaded.io.netty.channel.socket.SocketChannel;
import org.bithon.shaded.io.netty.channel.socket.nio.NioSocketChannel;
import org.bithon.shaded.io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.bithon.shaded.io.netty.handler.codec.LengthFieldPrepender;
import org.bithon.shaded.io.netty.util.concurrent.Future;

import java.io.Closeable;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Should only be used at the client side
 *
 * @author frankchen
 */
public class ClientChannel implements IChannelWriter, Closeable {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(ClientChannel.class);
    private final Bootstrap bootstrap;
    private final AtomicReference<Channel> channel = new AtomicReference<>();
    private final IEndPointProvider endPointProvider;
    private final ServiceRegistry serviceRegistry = new ServiceRegistry();
    private NioEventLoopGroup bossGroup;
    private final Duration retryInterval;
    private final int maxRetry;

    /**
     * a logic name of the client, which could be used for the servers to find client instances
     */
    private final String appName;

    /**
     * unique id of client application
     */
    private final Headers headers = new Headers();

    private long connectionTimestamp;

    private InvocationManager invocationManager;

    /**
     * It's better to use {@link ClientChannelBuilder} to instantiate the instance
     *
     * @param nWorkerThreads if it's 0, worker threads will be default to Runtime.getRuntime().availableProcessors() * 2
     */
    public ClientChannel(IEndPointProvider endPointProvider,
                         int nWorkerThreads,
                         int maxRetry,
                         Duration retryInterval,
                         String appName) {
        this.endPointProvider = endPointProvider;
        this.maxRetry = maxRetry;
        this.retryInterval = retryInterval;
        this.appName = appName;

        this.invocationManager = new InvocationManager();
        this.bossGroup = new NioEventLoopGroup(nWorkerThreads, NamedThreadFactory.of("brpc-client"));
        this.bootstrap = new Bootstrap();
        this.bootstrap.group(this.bossGroup)
                      .channel(NioSocketChannel.class)
                      .option(ChannelOption.SO_KEEPALIVE, true)
                      .handler(new ChannelInitializer<SocketChannel>() {
                          @Override
                          public void initChannel(SocketChannel ch) {
                              ChannelPipeline pipeline = ch.pipeline();
                              pipeline.addLast("frameDecoder",
                                               new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                              pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                              pipeline.addLast("decoder", new ServiceMessageInDecoder());
                              pipeline.addLast("encoder", new ServiceMessageOutEncoder(invocationManager));
                              pipeline.addLast(new ClientChannelManager());
                              pipeline.addLast(new ServiceMessageChannelHandler(serviceRegistry, invocationManager));
                          }
                      });
    }

    @Override
    public Channel getChannel() {
        return channel.get();
    }

    @Override
    public void write(Object obj) {
        Channel ch = channel.get();
        if (ch == null) {
            throw new ChannelException("Client channel is closed");
        }
        ch.writeAndFlush(obj);
    }

    @Override
    public synchronized void connect() {
        if (bossGroup == null) {
            throw new IllegalStateException("client channel has been shutdown");
        }
        if (channel.get() == null) {
            doConnect(maxRetry);
        }
    }

    @Override
    public synchronized void disconnect() {
        Channel ch = channel.getAndSet(null);
        if (ch != null) {
            ch.flush().disconnect();
        }
        connectionTimestamp = 0;
    }

    @Override
    public long getConnectionLifeTime() {
        return connectionTimestamp > 0 ? System.currentTimeMillis() - connectionTimestamp : 0;
    }

    @Override
    public void close() {
        if (this.bossGroup != null) {
            try {
                this.bossGroup.shutdownGracefully().sync();
            } catch (InterruptedException ignored) {
            }
        }
        this.bossGroup = null;
        this.channel.getAndSet(null);
    }

    private void doConnect(int maxRetry) {
        EndPoint endpoint = null;
        for (int i = 0; i < maxRetry; i++) {
            endpoint = endPointProvider.getEndpoint();
            try {
                Future<?> connectFuture = bootstrap.connect(endpoint.getHost(), endpoint.getPort());
                connectFuture.await(200, TimeUnit.MILLISECONDS);
                if (connectFuture.isSuccess()) {
                    connectionTimestamp = System.currentTimeMillis();
                    LOG.info("Successfully connected to remote service at [{}:{}]", endpoint.getHost(), endpoint.getPort());
                    return;
                }
                int leftCount = maxRetry - i - 1;
                if (leftCount > 0) {
                    LOG.warn("Unable to connect to remote service at [{}:{}]. Left retry count:{}",
                             endpoint.getHost(),
                             endpoint.getPort(),
                             maxRetry - i - 1);
                    Thread.sleep(retryInterval.toMillis());
                }
            } catch (InterruptedException ignored) {
            }
        }
        throw new CallerSideException("Unable to connect to remote service at [%s:%d]", endpoint.getHost(), endpoint.getPort());
    }

    public void bindService(Object serviceImpl) {
        serviceRegistry.addService(serviceImpl);
    }

    public <T> T getRemoteService(Class<T> serviceType) {
        // check service exist at first
        IServiceRegistry serviceRegistry = ServiceStubFactory.create(this.appName,
                                                                     Headers.EMPTY,
                                                                     this,
                                                                     IServiceRegistry.class,
                                                                     this.invocationManager);
        String serviceName = ServiceRegistryItem.getServiceName(serviceType);
        if (!serviceRegistry.contains(serviceName)) {
            throw new ServiceNotFoundException(serviceName);
        }

        return ServiceStubFactory.create(this.appName, this.headers, this, serviceType, this.invocationManager);
    }

    public void setHeader(String name, String value) {
        synchronized (this.headers) {
            this.headers.put(name, value);
        }
    }

    class ClientChannelManager extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            ClientChannel.this.channel.getAndSet(ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            ClientChannel.this.channel.getAndSet(null);
            super.channelInactive(ctx);
        }
    }
}
