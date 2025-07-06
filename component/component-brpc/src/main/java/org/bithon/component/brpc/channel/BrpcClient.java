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
import org.bithon.component.brpc.message.out.ServiceRequestMessageOut;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.shaded.io.netty.bootstrap.Bootstrap;
import org.bithon.shaded.io.netty.channel.Channel;
import org.bithon.shaded.io.netty.channel.ChannelFuture;
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

import java.io.Closeable;
import java.time.Duration;
import java.util.Map;
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
    private final Duration retryBackoff;
    private final int maxRetry;

    /**
     * a logic name of the client, which could be used for the servers to find client instances
     */
    private final String appName;
    private final String clientId;

    /**
     * unique id of client application
     */
    private final Headers headers = new Headers();

    private long connectionTimestamp;

    private final InvocationManager invocationManager;

    /**
     * Use {@link BrpcClientBuilder} to create instance.
     */
    BrpcClient(BrpcClientBuilder builder) {
        Preconditions.checkIfTrue(StringUtils.hasText("appName"), "appName can't be blank.");

        this.server = Preconditions.checkArgumentNotNull("server", builder.server);
        this.maxRetry = Math.max(1, builder.maxRetry);
        this.retryBackoff = builder.retryBackoff;
        this.appName = builder.appName;
        this.clientId = builder.clientId;

        this.invocationManager = new InvocationManager();
        this.bossGroup = new NioEventLoopGroup(builder.ioThreads, NamedThreadFactory.daemonThreadFactory("brpc-c-io-" + builder.clientId));
        this.bootstrap = new Bootstrap().group(this.bossGroup)
                                        .channel(NioSocketChannel.class)
                                        .option(ChannelOption.SO_KEEPALIVE, builder.keepAlive)
                                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) builder.connectionTimeout.toMillis())
                                        .handler(new ChannelInitializer<SocketChannel>() {
                                            @Override
                                            public void initChannel(SocketChannel ch) {
                                                ChannelPipeline pipeline = ch.pipeline();
                                                pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                                                pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                                                pipeline.addLast("decoder", new ServiceMessageInDecoder());
                                                pipeline.addLast("encoder", new ServiceMessageOutEncoder(invocationManager));
                                                pipeline.addLast(new ClientChannelManager());
                                                pipeline.addLast(new ServiceMessageChannelHandler(builder.clientId, serviceRegistry, builder.executor, invocationManager));
                                            }
                                        });

        if (builder.lowMaterMark > 0 && builder.highMaterMark > 0) {
            this.bootstrap.option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(builder.lowMaterMark, builder.highMaterMark));
        }

        if (builder.headers != null) {
            for (Map.Entry<String, String> entry : builder.headers.entrySet()) {
                String k = entry.getKey();
                String v = entry.getValue();
                this.setHeader(k, v);
            }
        }
    }

    @Override
    public void writeAsync(ServiceRequestMessageOut serviceRequest) {
        Channel ch = channelRef.get();
        if (ch == null) {
            throw new ChannelException("Client channel is closed");
        }
        ch.writeAndFlush(serviceRequest);
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
        close(2, 15, TimeUnit.SECONDS);
    }

    /**
     * Close the client with custom shutdown timeouts.
     * This is useful for tests that need faster shutdown.
     *
     * @param quietPeriod the quiet period for graceful shutdown
     * @param timeout     the maximum time to wait for shutdown
     * @param unit        the time unit
     */
    public void close(long quietPeriod, long timeout, TimeUnit unit) {
        if (this.bossGroup != null) {
            try {
                this.bossGroup.shutdownGracefully(quietPeriod, timeout, unit).sync();
            } catch (InterruptedException ignored) {
            }
        }
        this.bossGroup = null;
        this.channelRef.getAndSet(null);
    }

    /**
     * Fast shutdown for tests - uses minimal timeouts
     */
    public void fastClose() {
        close(100, 500, TimeUnit.MILLISECONDS);
    }

    private void doConnect(int maxRetry) {
        EndPoint server = null;
        for (int i = 0; i < maxRetry; i++) {
            server = this.server.getEndpoint();
            try {
                ChannelFuture connectFuture = bootstrap.connect(server.getHost(), server.getPort())
                                                       .awaitUninterruptibly();
                if (connectFuture.isSuccess()) {
                    connectionTimestamp = System.currentTimeMillis();

                    // Directly update the ref so that we can use the channel immediately
                    channelRef.getAndSet(connectFuture.channel());

                    LOG.info("Successfully connected to remote service at [{}:{}]", server.getHost(), server.getPort());
                    return;
                }
                int leftCount = maxRetry - i - 1;
                if (leftCount > 0) {
                    LOG.warn("Unable to connect to remote service at [{}:{}]. Left retry count:{}",
                             server.getHost(),
                             server.getPort(),
                             maxRetry - i - 1);
                    Thread.sleep(retryBackoff.toMillis());
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
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            LOG.info("[{}] Channel {} to {} is active",
                     BrpcClient.this.clientId,
                     ctx.channel().id().asLongText(),
                     ctx.channel().remoteAddress());

            BrpcClient.this.channelRef.getAndSet(ctx.channel());

            super.channelActive(ctx);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            LOG.info("[{}] Channel {} is inactive",
                     BrpcClient.this.clientId,
                     ctx.channel().id().asLongText());

            BrpcClient.this.channelRef.getAndSet(null);

            // Reset the connection timestamp so that we know that the connection is not connected
            BrpcClient.this.connectionTimestamp = 0;

            super.channelInactive(ctx);
        }
    }
}
