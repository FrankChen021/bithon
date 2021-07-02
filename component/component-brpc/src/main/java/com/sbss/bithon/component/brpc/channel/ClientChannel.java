/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.component.brpc.channel;

import com.sbss.bithon.component.brpc.ServiceRegistry;
import com.sbss.bithon.component.brpc.endpoint.EndPoint;
import com.sbss.bithon.component.brpc.endpoint.IEndPointProvider;
import com.sbss.bithon.component.brpc.endpoint.SingleEndPointProvider;
import com.sbss.bithon.component.brpc.exception.ServiceClientException;
import com.sbss.bithon.component.brpc.exception.ServiceInvocationException;
import com.sbss.bithon.component.brpc.invocation.ServiceStubFactory;
import com.sbss.bithon.component.brpc.message.in.ServiceMessageInDecoder;
import com.sbss.bithon.component.brpc.message.out.ServiceMessageOutEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Should only be used at the client side
 */
public class ClientChannel implements IChannelWriter, Closeable {
    private static final Logger log = LoggerFactory.getLogger(ClientChannel.class);

    //
    // channel
    //
    public static final int MAX_RETRY = 30;
    private final Bootstrap bootstrap;
    private final AtomicReference<Channel> channel = new AtomicReference<>();
    private final IEndPointProvider endPointProvider;
    private final ServiceRegistry serviceRegistry = new ServiceRegistry();
    private NioEventLoopGroup bossGroup;
    private Duration retryInterval;
    private int maxRetry;

    public ClientChannel(String host, int port) {
        this(new SingleEndPointProvider(host, port));
    }

    public ClientChannel(IEndPointProvider endPointProvider) {
        this.endPointProvider = endPointProvider;
        this.maxRetry = MAX_RETRY;
        this.retryInterval = Duration.ofMillis(100);

        bossGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(bossGroup)
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
                         pipeline.addLast("encoder", new ServiceMessageOutEncoder());
                         pipeline.addLast(new ClientChannelManager());
                         pipeline.addLast(new ServiceMessageChannelHandler(serviceRegistry));
                     }
                 });
    }

    @Override
    public Channel getChannel() {
        return channel.get();
    }

    @Override
    public void writeAndFlush(Object obj) {
        Channel ch = channel.get();
        if (ch == null) {
            throw new ServiceInvocationException("Client channel is closed");
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

    public ClientChannel configureRetry(int maxRetry, Duration interval) {
        this.maxRetry = maxRetry;
        this.retryInterval = interval;
        return this;
    }

    @Override
    public void close() {
        try {
            this.bossGroup.shutdownGracefully().sync();
        } catch (InterruptedException ignored) {
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
                    log.info("Successfully connected to server({}:{})", endpoint.getHost(), endpoint.getPort());
                    return;
                }
                int leftCount = maxRetry - i - 1;
                if (leftCount > 0) {
                    log.warn("Unable to connect to server({}:{})。Left retry count:{}",
                             endpoint.getHost(),
                             endpoint.getPort(),
                             maxRetry - i - 1);
                    Thread.sleep(retryInterval.toMillis());
                }
            } catch (InterruptedException e) {
                throw new ServiceClientException("Unable to connect to server, interrupted");
            }
        }
        throw new ServiceClientException("Unable to connect to server({}:{})", endpoint.getHost(), endpoint.getPort());
    }

    public ClientChannel bindService(Class<?> serviceType, Object serviceImpl) {
        serviceRegistry.addService(serviceType, serviceImpl);
        return this;
    }

    public <T> T getRemoteService(Class<T> serviceType) {
        return ServiceStubFactory.create(this, serviceType);
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
