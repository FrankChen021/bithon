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
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.shaded.io.netty.bootstrap.Bootstrap;
import org.bithon.shaded.io.netty.channel.Channel;
import org.bithon.shaded.io.netty.channel.ChannelHandler;
import org.bithon.shaded.io.netty.channel.ChannelHandlerContext;
import org.bithon.shaded.io.netty.channel.ChannelInboundHandlerAdapter;
import org.bithon.shaded.io.netty.channel.ChannelInitializer;
import org.bithon.shaded.io.netty.channel.ChannelOption;
import org.bithon.shaded.io.netty.channel.ChannelPipeline;
import org.bithon.shaded.io.netty.channel.WriteBufferWaterMark;
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
public class BrpcClient implements IBrpcChannel, Closeable {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(BrpcClient.class);
    private final Bootstrap bootstrap;
    private final AtomicReference<Channel> channelRef = new AtomicReference<>();
    private final IEndPointProvider server;
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

    private final InvocationManager invocationManager;

    /**
     * Use {@link BrpcClientBuilder} to create instance.
     *
     * @param nWorkerThreads if it's 0, worker threads will be default to Runtime.getRuntime().availableProcessors() * 2
     */
    BrpcClient(IEndPointProvider server,
               int nWorkerThreads,
               int maxRetry,
               Duration retryInterval,
               String appName,
               String clientId) {
        Preconditions.checkIfTrue(StringUtils.hasText("appName"), "appName can't be blank.");
        Preconditions.checkIfTrue(maxRetry > 0, "maxRetry must be at least 1.");

        this.server = Preconditions.checkArgumentNotNull("server", server);
        this.maxRetry = maxRetry;
        this.retryInterval = retryInterval;
        this.appName = appName;

        this.invocationManager = new InvocationManager();
        this.bossGroup = new NioEventLoopGroup(nWorkerThreads, NamedThreadFactory.of("brpc-c-work-" + clientId));
        this.bootstrap = new Bootstrap();
        this.bootstrap.group(this.bossGroup)
                      .channel(NioSocketChannel.class)
                      .option(ChannelOption.SO_KEEPALIVE, true)
                      .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(WriteBufferWaterMark.DEFAULT.low(), 1024 * 1024))
                      .handler(new ChannelInitializer<SocketChannel>() {
                          @Override
                          public void initChannel(SocketChannel ch) {
                              ChannelPipeline pipeline = ch.pipeline();
                              pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                              pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                              pipeline.addLast("decoder", new ServiceMessageInDecoder());
                              pipeline.addLast("encoder", new ServiceMessageOutEncoder(invocationManager));
                              pipeline.addLast(new ClientChannelManager());
                              pipeline.addLast(new ServiceMessageChannelHandler(serviceRegistry, invocationManager));
                          }
                      });
    }

    @Override
    public void writeAsync(Object obj) {
        Channel ch = channelRef.get();
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
        if (channelRef.get() == null) {
            doConnect(maxRetry);
        }
    }

    @Override
    public synchronized void disconnect() {
        Channel ch = channelRef.getAndSet(null);
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
    public boolean isActive() {
        Channel ch = channelRef.get();
        return ch != null && ch.isActive();
    }

    @Override
    public boolean isWritable() {
        Channel ch = channelRef.get();
        return ch != null && ch.isWritable();
    }

    @Override
    public EndPoint getRemoteAddress() {
        Channel ch = channelRef.get();
        return ch != null ? EndPoint.of(ch.remoteAddress()) : null;
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
        this.channelRef.getAndSet(null);
    }

    private void doConnect(int maxRetry) {
        EndPoint server = null;
        for (int i = 0; i < maxRetry; i++) {
            server = this.server.getEndpoint();
            try {
                Future<?> connectFuture = bootstrap.connect(server.getHost(), server.getPort());
                connectFuture.await(200, TimeUnit.MILLISECONDS);
                if (connectFuture.isSuccess()) {
                    connectionTimestamp = System.currentTimeMillis();
                    LOG.info("Successfully connected to remote service at [{}:{}]", server.getHost(), server.getPort());
                    return;
                }
                int leftCount = maxRetry - i - 1;
                if (leftCount > 0) {
                    LOG.warn("Unable to connect to remote service at [{}:{}]. Left retry count:{}",
                             server.getHost(),
                             server.getPort(),
                             maxRetry - i - 1);
                    Thread.sleep(retryInterval.toMillis());
                }
            } catch (InterruptedException ignored) {
            }
        }
        throw new CallerSideException("Unable to connect to remote service at [%s:%d]", server.getHost(), server.getPort());
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

    @ChannelHandler.Sharable
    class ClientChannelManager extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            BrpcClient.this.channelRef.getAndSet(ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            BrpcClient.this.channelRef.getAndSet(null);

            // Reset the connection timestamp so that we know that the connection is not connected
            BrpcClient.this.connectionTimestamp = 0;
            super.channelInactive(ctx);
        }
    }
}
