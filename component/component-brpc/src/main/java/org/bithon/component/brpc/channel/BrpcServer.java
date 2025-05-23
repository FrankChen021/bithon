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
import org.bithon.component.brpc.message.out.ServiceRequestMessageOut;
import org.bithon.component.commons.concurrency.NamedThreadFactory;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.shaded.io.netty.bootstrap.ServerBootstrap;
import org.bithon.shaded.io.netty.buffer.PooledByteBufAllocator;
import org.bithon.shaded.io.netty.channel.Channel;
import org.bithon.shaded.io.netty.channel.ChannelHandler;
import org.bithon.shaded.io.netty.channel.ChannelHandlerContext;
import org.bithon.shaded.io.netty.channel.ChannelInboundHandlerAdapter;
import org.bithon.shaded.io.netty.channel.ChannelInitializer;
import org.bithon.shaded.io.netty.channel.ChannelOption;
import org.bithon.shaded.io.netty.channel.ChannelPipeline;
import org.bithon.shaded.io.netty.channel.WriteBufferWaterMark;
import org.bithon.shaded.io.netty.channel.nio.NioEventLoopGroup;
import org.bithon.shaded.io.netty.channel.socket.nio.NioServerSocketChannel;
import org.bithon.shaded.io.netty.channel.socket.nio.NioSocketChannel;
import org.bithon.shaded.io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.bithon.shaded.io.netty.handler.codec.LengthFieldPrepender;
import org.bithon.shaded.io.netty.handler.timeout.IdleState;
import org.bithon.shaded.io.netty.handler.timeout.IdleStateEvent;
import org.bithon.shaded.io.netty.handler.timeout.IdleStateHandler;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author frankchen
 */
public class BrpcServer implements Closeable {

    private final ServerBootstrap serverBootstrap;
    private final NioEventLoopGroup acceptorGroup;
    private final NioEventLoopGroup ioGroup;

    private final ServiceRegistry serviceRegistry = new ServiceRegistry();
    private final SessionManager sessionManager;
    private final InvocationManager invocationManager;

    BrpcServer(BrpcServerBuilder builder) {
        Preconditions.checkNotNull(builder.serverId, "serverId must be set");

        this.acceptorGroup = new NioEventLoopGroup(1, NamedThreadFactory.nonDaemonThreadFactory("brpc-server-" + builder.serverId));
        this.ioGroup = new NioEventLoopGroup(builder.ioThreads, NamedThreadFactory.nonDaemonThreadFactory("brpc-s-work-" + builder.serverId));

        this.invocationManager = new InvocationManager();
        this.sessionManager = new SessionManager(this.invocationManager);

        final Executor executor = builder.executor;
        this.serverBootstrap = new ServerBootstrap()
            .group(acceptorGroup, ioGroup)
            .channel(NioServerSocketChannel.class)
            .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .option(ChannelOption.SO_BACKLOG, builder.backlog)
            .option(ChannelOption.SO_REUSEADDR, true)
            .childOption(ChannelOption.SO_KEEPALIVE, false)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .childHandler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
                    pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                    pipeline.addLast("decoder", new ServiceMessageInDecoder());
                    pipeline.addLast("encoder", new ServiceMessageOutEncoder(invocationManager));
                    pipeline.addLast(new IdleStateHandler(builder.idleSeconds, 0, 0));
                    pipeline.addLast(sessionManager);
                    pipeline.addLast(new ServiceMessageChannelHandler(builder.serverId, serviceRegistry, executor, invocationManager));
                }
            });

        if (builder.lowWaterMark > 0 && builder.highWaterMark > 0) {
            this.serverBootstrap.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(builder.lowWaterMark, builder.highWaterMark));
        }
    }

    /**
     * bind current service provider to this channel
     * NOTE:
     * 1. only those methods defined in the interface will be bound
     * 2. method name can be the same, but the methods with same name must use {@link BrpcMethod} to give alias name to each method
     */
    public BrpcServer bindService(Object impl) {
        serviceRegistry.addService(impl);
        return this;
    }

    public BrpcServer start(int port) {
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
        close(2, 15, TimeUnit.SECONDS);
    }
    
    /**
     * Close the server with custom shutdown timeouts.
     * This is useful for tests that need faster shutdown.
     * 
     * @param quietPeriod the quiet period for graceful shutdown
     * @param timeout the maximum time to wait for shutdown
     * @param unit the time unit
     */
    public void close(long quietPeriod, long timeout, TimeUnit unit) {
        try {
            acceptorGroup.shutdownGracefully(quietPeriod, timeout, unit).sync();
        } catch (InterruptedException ignored) {
        }
        try {
            ioGroup.shutdownGracefully(quietPeriod, timeout, unit).sync();
        } catch (InterruptedException ignored) {
        }
    }
    
    /**
     * Fast shutdown for tests - uses minimal timeouts
     */
    public void fastClose() {
        close(100, 500, TimeUnit.MILLISECONDS);
    }

    public List<Session> getSessions() {
        return sessionManager.getSessions();
    }

    /**
     * {@link SessionNotFoundException} will be thrown if the remote application is not connected to this server
     */
    public Session getSession(String remoteAppId) {
        return sessionManager.getSessions()
                             .stream()
                             .filter(s -> remoteAppId.equals(s.getRemoteAttribute(Headers.HEADER_APP_ID, s.getRemoteEndpoint())))
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
                             .filter(s -> appName.equals(s.getRemoteApplicationName()))
                             .map(s -> s.getRemoteService(serviceClass, 5_000))
                             .collect(Collectors.toList());
    }

    public static class Session {
        private final Channel channel;

        private final String localEndpoint;
        /**
         * Socket endpoint of client
         */
        private final String remoteEndpoint;
        private final InvocationManager invocationManager;

        private final String remoteApplicationName;
        private final long sessionStartTimestamp;

        /**
         * Attributes from remote side
         */
        private Map<String, String> remoteAttribute = Collections.emptyMap();

        private Session(String applicationName, Channel channel, InvocationManager invocationManager) {
            this.sessionStartTimestamp = System.currentTimeMillis();
            this.channel = channel;
            this.remoteApplicationName = applicationName;
            this.remoteEndpoint = EndPoint.of(channel.remoteAddress()).toString();
            this.localEndpoint = EndPoint.of(channel.localAddress()).toString();
            this.invocationManager = invocationManager;
        }

        public String getRemoteApplicationName() {
            return remoteApplicationName;
        }

        public String getRemoteEndpoint() {
            return remoteEndpoint;
        }

        public String getLocalEndpoint() {
            return localEndpoint;
        }

        public void setRemoteAttribute(Map<String, String> attributes) {
            this.remoteAttribute = Collections.unmodifiableMap(new HashMap<>(attributes));
        }

        public String getRemoteAttribute(String name) {
            return this.remoteAttribute.get(name);
        }

        public String getRemoteAttribute(String name, String defaultValue) {
            return this.remoteAttribute.getOrDefault(name, defaultValue);
        }

        public <T> T getRemoteService(Class<T> serviceClass, int timeout) {
            return ServiceStubFactory.create("brpc-server",
                                             Headers.EMPTY,
                                             new Server2ClientChannel(channel, sessionStartTimestamp),
                                             serviceClass,
                                             timeout,
                                             invocationManager);
        }

        public LowLevelInvoker getLowLevelInvoker() {
            return new LowLevelInvoker(new Server2ClientChannel(channel, sessionStartTimestamp), invocationManager);
        }
    }

    /**
     * Theoretically, Session is a concept at both client and server side.
     * However, for the client, each client just has one session, it's not important to provide a session on the client side.
     * In contrast, one server manages multiple clients,
     * So session is used to wrap a connected channel to help the server side to call services implemented on the client side.
     */
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
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (!(msg instanceof ServiceRequestMessageIn)) {
                super.channelRead(ctx, msg);
                return;
            }

            ServiceRequestMessageIn request = (ServiceRequestMessageIn) msg;

            // Create a session only if the ServiceRequestMessageIn has been successfully decoded
            Session session = sessions.computeIfAbsent(ctx.channel().id().asLongText(),
                                                       key -> new Session(request.getApplicationName(), ctx.channel(), invocationManager));

            // Update additional attributes
            session.setRemoteAttribute(request.getHeaders());

            super.channelRead(ctx, msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            sessions.remove(ctx.channel().id().asLongText());
            super.channelInactive(ctx);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (!(evt instanceof IdleStateEvent)) {
                return;
            }

            if (IdleState.READER_IDLE.equals(((IdleStateEvent) evt).state())) {
                ctx.channel().close();

                // Since the above call is async, we remove the session immediately to reflect the timeout at application side
                Session session = sessions.remove(ctx.channel().id().asLongText());
                if (session != null) {
                    LoggerFactory.getLogger(BrpcServer.class)
                                 .info("Close idle connection: {} -> {}, session life time: {}ms",
                                       session.remoteEndpoint,
                                       session.localEndpoint,
                                       System.currentTimeMillis() - session.sessionStartTimestamp);
                }
            }
        }
    }

    private static class Server2ClientChannel implements IBrpcChannel {
        private final Channel channel;
        private final long sessionStartTimestamp;

        public Server2ClientChannel(Channel channel, long sessionStartTimestamp) {
            this.channel = channel;
            this.sessionStartTimestamp = sessionStartTimestamp;
        }

        @Override
        public long getConnectionLifeTime() {
            // Treat session start timestamp as the timestamp when the connection is established
            return System.currentTimeMillis() - sessionStartTimestamp;
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
        public void writeAsync(ServiceRequestMessageOut serviceRequest) {
            channel.writeAndFlush(serviceRequest);
        }
    }
}
