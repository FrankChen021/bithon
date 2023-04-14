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
import org.bithon.component.brpc.exception.SessionNotFoundException;
import org.bithon.component.brpc.invocation.InvocationManager;
import org.bithon.component.brpc.invocation.LowLevelInvoker;
import org.bithon.component.brpc.invocation.ServiceStubFactory;
import org.bithon.component.brpc.message.Headers;
import org.bithon.component.brpc.message.in.ServiceMessageInDecoder;
import org.bithon.component.brpc.message.in.ServiceRequestMessageIn;
import org.bithon.component.brpc.message.out.ServiceMessageOutEncoder;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.shaded.io.netty.bootstrap.ServerBootstrap;
import org.bithon.shaded.io.netty.buffer.PooledByteBufAllocator;
import org.bithon.shaded.io.netty.channel.Channel;
import org.bithon.shaded.io.netty.channel.ChannelHandler;
import org.bithon.shaded.io.netty.channel.ChannelHandlerContext;
import org.bithon.shaded.io.netty.channel.ChannelInboundHandlerAdapter;
import org.bithon.shaded.io.netty.channel.ChannelInitializer;
import org.bithon.shaded.io.netty.channel.ChannelOption;
import org.bithon.shaded.io.netty.channel.ChannelPipeline;
import org.bithon.shaded.io.netty.channel.nio.NioEventLoopGroup;
import org.bithon.shaded.io.netty.channel.socket.nio.NioServerSocketChannel;
import org.bithon.shaded.io.netty.channel.socket.nio.NioSocketChannel;
import org.bithon.shaded.io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.bithon.shaded.io.netty.handler.codec.LengthFieldPrepender;
import org.bithon.shaded.io.netty.util.internal.StringUtil;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author frankchen
 */
public class BrpcServer implements Closeable {

    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup workerGroup;
    private final ServiceRegistry serviceRegistry = new ServiceRegistry();

    private final SessionManager sessionManager;

    private final InvocationManager invocationManager;

    public BrpcServer() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public BrpcServer(int nThreadCount) {
        this.bossGroup = new NioEventLoopGroup(1, NamedThreadFactory.of("brpc-server"));
        this.workerGroup = new NioEventLoopGroup(nThreadCount, NamedThreadFactory.of("brpc-s-worker"));

        this.invocationManager = new InvocationManager();
        this.sessionManager = new SessionManager(this.invocationManager);
    }

    /**
     * bind current service provider to this channel
     * NOTE:
     * 1. only those methods defined in interface will be bound
     * 2. methods name can be the same, but the methods with same name must use {@link BrpcMethod} to give alias name to each method
     */
    public BrpcServer bindService(Object impl) {
        serviceRegistry.addService(impl);
        return this;
    }

    public BrpcServer start(int port) {

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, false)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                        pipeline.addLast("decoder", new ServiceMessageInDecoder());
                        pipeline.addLast("encoder", new ServiceMessageOutEncoder(invocationManager));
                        pipeline.addLast(sessionManager);
                        pipeline.addLast(new ServiceMessageChannelHandler(serviceRegistry, invocationManager));
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

    public List<Session> getSessions() {
        return sessionManager.getSessions();
    }

    public Session getSession(String remoteAppId) {
        return sessionManager.getSessions()
                             .stream()
                             .filter(s -> remoteAppId.equals(s.getAppId()))
                             .findFirst()
                             .orElseThrow(() -> new SessionNotFoundException("Can't find any connected remote application [%s] on this server.", remoteAppId));
    }

    public <T> T getRemoteService(String remoteAppId, Class<T> serviceClass) {
        return getRemoteService(remoteAppId, serviceClass, 5000);
    }

    /**
     * @param timeout in milliseconds
     */
    public <T> T getRemoteService(String remoteAppId, Class<T> serviceClass, int timeout) {
        return getSession(remoteAppId).getRemoteService(serviceClass, timeout);
    }

    public <T> List<T> getRemoteServices(String appName, Class<T> serviceClass) {
        return sessionManager.getSessions()
                             .stream()
                             .filter(s -> appName.equals(s.getAppName()))
                             .map(s -> s.getRemoteService(serviceClass, 5_000))
                             .collect(Collectors.toList());
    }

    public static class Session {
        private final Channel channel;

        /**
         * Socket endpoint of client
         */
        private final EndPoint endpoint;
        private final InvocationManager invocationManager;
        private String appName;

        /**
         * a unique id for client
         */
        private String appId;

        private String clientVersion;

        private Session(Channel channel, InvocationManager invocationManager) {
            this.channel = channel;
            this.endpoint = EndPoint.of(channel.remoteAddress());
            this.invocationManager = invocationManager;

            // appId default to endpoint at first
            this.appId = this.endpoint.toString();
        }

        public String getAppName() {
            return appName;
        }

        public Channel getChannel() {
            return channel;
        }

        public EndPoint getEndpoint() {
            return endpoint;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getClientVersion() {
            return clientVersion;
        }

        public void setClientVersion(String clientVersion) {
            this.clientVersion = clientVersion;
        }

        public <T> T getRemoteService(Class<T> serviceClass, int timeout) {
            return ServiceStubFactory.create(null,
                                             Headers.EMPTY,
                                             new Server2ClientChannel(channel),
                                             serviceClass,
                                             timeout,
                                             invocationManager);
        }

        public LowLevelInvoker getLowLevelInvoker() {
            return new LowLevelInvoker(new Server2ClientChannel(channel), invocationManager);
        }
    }

    @ChannelHandler.Sharable
    private static class SessionManager extends ChannelInboundHandlerAdapter {
        private final InvocationManager invocationManager;

        private final Map<String, Session> sessions = new ConcurrentHashMap<>();

        private SessionManager(InvocationManager invocationManager) {
            this.invocationManager = invocationManager;
        }

        public List<Session> getSessions() {
            return new ArrayList<>(sessions.values());
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            sessions.computeIfAbsent(ctx.channel().id().asLongText(), key -> new Session(ctx.channel(), invocationManager));
            super.channelActive(ctx);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (!(msg instanceof ServiceRequestMessageIn)) {
                super.channelRead(ctx, msg);
                return;
            }

            Session session = sessions.get(ctx.channel().id().asLongText());
            if (session != null) {

                // Update appName
                ServiceRequestMessageIn request = (ServiceRequestMessageIn) msg;
                if (!StringUtil.isNullOrEmpty(request.getAppName())) {
                    session.appName = request.getAppName();
                }

                String appId = request.getHeaders().get(Headers.HEADER_APP_ID);
                if (!StringUtils.isEmpty(appId)) {
                    session.setAppId(appId);
                }

                String buildId = request.getHeaders().get(Headers.HEADER_VERSION);
                if (!StringUtils.isEmpty(buildId)) {
                    session.setClientVersion(buildId);
                }
            }

            super.channelRead(ctx, msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            sessions.remove(ctx.channel().id().asLongText());
            super.channelInactive(ctx);
        }
    }

    private static class Server2ClientChannel implements IBrpcChannel {
        private final Channel channel;

        public Server2ClientChannel(Channel channel) {
            this.channel = channel;
        }

        @Override
        public long getConnectionLifeTime() {
            // not support for a server channel now
            return 0;
        }

        @Override
        public boolean isActive() {
            return channel.isActive();
        }

        @Override
        public boolean isWritable() {
            return channel.isWritable();
        }

        @Override
        public EndPoint getRemoteAddress() {
            SocketAddress addr = channel.remoteAddress();
            return addr != null ? EndPoint.of(addr) : null;
        }

        @Override
        public void writeAsync(Object obj) {
            channel.writeAndFlush(obj);
        }
    }
}