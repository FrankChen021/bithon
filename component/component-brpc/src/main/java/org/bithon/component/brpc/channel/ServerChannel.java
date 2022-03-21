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

import org.bithon.component.brpc.BrpcMethod;
import org.bithon.component.brpc.ServiceRegistry;
import org.bithon.component.brpc.endpoint.EndPoint;
import org.bithon.component.brpc.invocation.ServiceStubFactory;
import org.bithon.component.brpc.message.ServiceMessage;
import org.bithon.component.brpc.message.ServiceMessageType;
import org.bithon.component.brpc.message.in.ServiceMessageInDecoder;
import org.bithon.component.brpc.message.in.ServiceRequestMessageIn;
import org.bithon.component.brpc.message.out.ServiceMessageOutEncoder;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import shaded.io.netty.bootstrap.ServerBootstrap;
import shaded.io.netty.buffer.PooledByteBufAllocator;
import shaded.io.netty.channel.Channel;
import shaded.io.netty.channel.ChannelHandler;
import shaded.io.netty.channel.ChannelHandlerContext;
import shaded.io.netty.channel.ChannelInboundHandlerAdapter;
import shaded.io.netty.channel.ChannelInitializer;
import shaded.io.netty.channel.ChannelOption;
import shaded.io.netty.channel.ChannelPipeline;
import shaded.io.netty.channel.nio.NioEventLoopGroup;
import shaded.io.netty.channel.socket.nio.NioServerSocketChannel;
import shaded.io.netty.channel.socket.nio.NioSocketChannel;
import shaded.io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import shaded.io.netty.handler.codec.LengthFieldPrepender;
import shaded.io.netty.util.Attribute;
import shaded.io.netty.util.AttributeKey;
import shaded.io.netty.util.internal.StringUtil;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServerChannel implements Closeable {

    private static final AttributeKey<String> ATTR_CLIENT_NAME = AttributeKey.valueOf("client.name");

    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup workerGroup;
    private final ServiceRegistry serviceRegistry = new ServiceRegistry();

    /**
     * 服务端的请求直接在worker线程中处理，无需单独定义线程池
     */
    private final ClientChannelManager clientChannelManager = new ClientChannelManager();

    public ServerChannel() {
        this(8);
    }

    public ServerChannel(int nThreadCount) {
        bossGroup = new NioEventLoopGroup(1, NamedThreadFactory.of("brpc-server"));
        workerGroup = new NioEventLoopGroup(nThreadCount, NamedThreadFactory.of("brpc-s-worker"));
    }

    /**
     * bind current service provider to this channel
     * NOTE:
     * 1. only those methods defined in interface will be binded
     * 2. methods name can be the same, but the methods with same name must use {@link BrpcMethod} to give alias name to each method
     */
    public ServerChannel bindService(Object impl) {
        serviceRegistry.addService(impl);
        return this;
    }

    public ServerChannel start(int port) {

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap
            .group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            //设置底层使用对象池减少内存开销 提升GC效率
            .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            //服务端可连接队列数,对应TCP/IP协议listen函数中backlog参数
            .option(ChannelOption.SO_BACKLOG, 1024)
            //设置TCP长连接,一般如果两个小时内没有数据的通信时,TCP会自动发送一个活动探测数据报文
            .childOption(ChannelOption.SO_KEEPALIVE, false)
            //将小的数据包包装成更大的帧进行传送，提高网络的负载
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childHandler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                    pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                    pipeline.addLast("decoder", new ServiceMessageInDecoder());
                    pipeline.addLast("encoder", new ServiceMessageOutEncoder());
                    pipeline.addLast(clientChannelManager);
                    pipeline.addLast(new ServiceMessageChannelHandler(serviceRegistry));
                }
            });
        try {
            serverBootstrap.bind(port).sync();
        } catch (InterruptedException e) {
            System.exit(-1);
        }
        Runtime.getRuntime().addShutdownHook(new Thread(this::close));
        return this;
    }

    public ServerChannel debug(boolean on) {
        return this;
    }

    @Override
    public void close() {
        try {
            bossGroup.shutdownGracefully().sync();
        } catch (InterruptedException ignored) {
        }
        try {
            workerGroup.shutdownGracefully().sync();
        } catch (InterruptedException ignored) {
        }
    }

    public Set<EndPoint> getClientEndpoints() {
        return clientChannelManager.getEndpoints();
    }

    public <T> T getRemoteService(EndPoint clientEndpoint, Class<T> serviceClass) {
        Channel channel = clientChannelManager.getChannel(clientEndpoint);
        if (channel == null) {
            return null;
        }

        return ServiceStubFactory.create(null,
                                         new Server2ClientChannelWriter(channel),
                                         serviceClass);
    }

    public <T> List<T> getRemoteService(String client, Class<T> serviceClass) {

        List<T> services = new ArrayList<>();
        this.clientChannelManager.getApp2Channels().getOrDefault(client, Collections.emptySet()).forEach(channel -> {
            T service = ServiceStubFactory.create(null,
                                                  new Server2ClientChannelWriter(channel),
                                                  serviceClass);
            services.add(service);
        });
        return services;
    }

    @ChannelHandler.Sharable
    private static class ClientChannelManager extends ChannelInboundHandlerAdapter {

        private final Map<EndPoint, Channel> endpoint2Channels = new ConcurrentHashMap<>();
        private final Map<String, Set<Channel>> app2Channels = new ConcurrentHashMap<>();

        public Set<EndPoint> getEndpoints() {
            return endpoint2Channels.keySet();
        }

        public Channel getChannel(EndPoint endpoint) {
            return endpoint2Channels.get(endpoint);
        }

        public Map<String, Set<Channel>> getApp2Channels() {
            return app2Channels;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            endpoint2Channels.computeIfAbsent(EndPoint.of(socketAddress), key -> ctx.channel());
            super.channelActive(ctx);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (!(msg instanceof ServiceMessage)) {
                // NO Need to handle messages
                return;
            }

            ServiceMessage message = (ServiceMessage) msg;
            if (message.getMessageType() != ServiceMessageType.CLIENT_REQUEST) {
                super.channelRead(ctx, msg);
                return;
            }

            ServiceRequestMessageIn request = (ServiceRequestMessageIn) message;
            if (StringUtil.isNullOrEmpty(request.getAppName())) {
                super.channelRead(ctx, msg);
                return;
            }

            Attribute<String> clientName = ctx.channel().attr(ATTR_CLIENT_NAME);
            if (clientName.get() == null && clientName.setIfAbsent(request.getAppName()) == null) {
                app2Channels.computeIfAbsent(request.getAppName(), client -> new HashSet<>()).add(ctx.channel());
            }

            super.channelRead(ctx, msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            endpoint2Channels.remove(EndPoint.of(socketAddress));

            Attribute<String> clientNameKey = ctx.channel().attr(ATTR_CLIENT_NAME);
            String clientName = clientNameKey.get();
            if (clientName != null) {
                Set<Channel> chs = app2Channels.get(clientName);
                chs.remove(ctx.channel());

                if (chs.isEmpty()) {
                    app2Channels.remove(clientName);
                }
            }

            super.channelInactive(ctx);
        }
    }

    private static class Server2ClientChannelWriter implements IChannelWriter {
        private final Channel channel;

        public Server2ClientChannelWriter(Channel channel) {
            this.channel = channel;
        }

        @Override
        public void connect() {
            //do nothing for a server channel
        }

        @Override
        public void disconnect() {
            //do nothing for a server channel
        }

        @Override
        public long getConnectionLifeTime() {
            // not support for a server channel now
            return 0;
        }

        @Override
        public Channel getChannel() {
            return channel;
        }

        @Override
        public void writeAndFlush(Object obj) {
            channel.writeAndFlush(obj);
        }
    }
}
